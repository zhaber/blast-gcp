/*===========================================================================
*
*                            PUBLIC DOMAIN NOTICE
*               National Center for Biotechnology Information
*
*  This software/database is a "United States Government Work" under the
*  terms of the United States Copyright Act.  It was written as part of
*  the author's official duties as a United States Government employee and
*  thus cannot be copyrighted.  This software/database is freely available
*  to the public for use. The National Library of Medicine and the U.S.
*  Government have not placed any restriction on its use or reproduction.
*
*  Although all reasonable efforts have been taken to ensure the accuracy
*  and reliability of the software and data, the NLM and the U.S.
*  Government do not and cannot warrant the performance or results that
*  may be obtained by using this software or data. The NLM and the U.S.
*  Government disclaim all warranties, express or implied, including
*  warranties of performance, merchantability or fitness for any particular
*  purpose.
*
*  Please cite the author in any work or product based on this material.
*
* ===========================================================================
*
*/

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import scala.Tuple2;

import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkFiles;
import org.apache.spark.HashPartitioner;

class GCP_BLAST_DRIVER extends Thread
{
    private final GCP_BLAST_SETTINGS settings;
    private JavaSparkContext sc;
    private JavaStreamingContext jssc;

    public GCP_BLAST_DRIVER( final GCP_BLAST_SETTINGS settings )
    {
        this.settings = settings;
    }

    public void stop_blast()
    {
        try
        {
            if ( jssc != null )
                jssc.stop( true, true );
        }
        catch ( Exception e )
        {
            System.out.println( "JavaStreamingContext.stop() : " + e );
        }
    }

    private void stream_version()
    {
        try
        {
            // we broadcast this list to all nodes
            Broadcast< Integer > NUM_WORKERS    = jssc.sparkContext().broadcast( settings.num_workers );
            Broadcast< String > LOG_HOST        = jssc.sparkContext().broadcast( settings.log_host );
            Broadcast< Integer > LOG_PORT       = jssc.sparkContext().broadcast( settings.log_port );
            Broadcast< String > SAVE_DIR        = jssc.sparkContext().broadcast( settings.save_dir );
            Broadcast< Boolean > LOG_REQUEST    = jssc.sparkContext().broadcast( settings.log_request );
            Broadcast< Boolean > LOG_JOB_START  = jssc.sparkContext().broadcast( settings.log_job_start );
            Broadcast< Boolean > LOG_JOB_DONE   = jssc.sparkContext().broadcast( settings.log_job_done );
            Broadcast< Boolean > LOG_FINAL      = jssc.sparkContext().broadcast( settings.log_final );

            // ***** STEP1: create a list of GCP_BLAST_PARTITION's on the master *****
            List< GCP_BLAST_PARTITION > db_sec_list = new ArrayList<>();
            for ( int i = 0; i < settings.num_db_partitions; i++ )
                db_sec_list.add( new GCP_BLAST_PARTITION( settings.db_location, settings.db_pattern, i, settings.flat_db_layout ) );

            // ***** STEP2: create a custom partitioner for the catesian - step ( STEP5 ) *****
            GCP_BLAST_CustomPartitioner myPartitioner = new GCP_BLAST_CustomPartitioner( settings.num_workers );

            // ***** STEP3: distribute the PARTITION-LIST *****
            JavaRDD< GCP_BLAST_PARTITION > DB_SEC = sc.parallelize( db_sec_list ).cache();

            // to get the sum of the size of all databases
			Integer numbases = DB_SEC.map( bp -> bp.getSize() ).reduce( ( x, y ) -> x + y );

            // ***** STEP4: establish the data-source ( listens on a socket for requests ) *****
            JavaDStream< String > REQ_STREAM = jssc.socketTextStream( settings.trigger_host, settings.trigger_port );
            REQ_STREAM.cache();

            // ***** STEP5: build the cartesion between Requests and Partitions, and repartition by custom partitioner *****
            JavaPairDStream< GCP_BLAST_PARTITION, String > JOINED_REQ_STREAM
                = REQ_STREAM.transformToPair( rdd ->
            {
                return DB_SEC.cartesian( rdd ).partitionBy( myPartitioner );
            } ).cache();

            // ***** STEP6: perform prelim-search on each job, creates multiple HSPs from each job *****
            JavaDStream< GCP_BLAST_HSP_LIST > SEARCH_RES = JOINED_REQ_STREAM.map( item ->
            {
                GCP_BLAST_PARTITION part = item._1();
                GCP_BLAST_REQUEST req = new GCP_BLAST_REQUEST( item._2() ); // REQ-LINE to REQUEST
                GCP_BLAST_HSP_LIST hsps = new GCP_BLAST_HSP_LIST( req, part );

                BlastJNI blaster = new BlastJNI ();
                // ++++++ this is the where the work happens on the worker-nodes ++++++

                if ( LOG_JOB_START.getValue() )
                    GCP_BLAST_SEND.send( LOG_HOST.getValue(), LOG_PORT.getValue(),
                                     String.format( "starting request: '%s' at '%s' ", req.req_id, part.db_spec ) );

                Integer count = 0;
                try
                {
                    //query, db_spec, program, params
                    String rid = req.req_id;
                    String[] search_res = blaster.jni_prelim_search( rid, req.query, part.db_spec, req.params );

                    count = search_res.length;
                
                    if ( LOG_JOB_DONE.getValue() )
                        GCP_BLAST_SEND.send( LOG_HOST.getValue(), LOG_PORT.getValue(),
                                         String.format( "request '%s'.'%s' done -> count = %d", rid, part.db_spec, count ) );

                    if ( count > 0 )
                    {
                        for ( String S : search_res )
                            hsps.add( new GCP_BLAST_HSP( S ) );
                    }
                }
                catch ( Exception e )
                {
                    GCP_BLAST_SEND.send( LOG_HOST.getValue(), LOG_PORT.getValue(),
                                         String.format( "request exeption: '%s on %s' for '%s'", e, req.toString(), part.toString() ) );
                }
                return hsps;
            } ).cache();

            JavaDStream< GCP_BLAST_RID_SCORE > SCORES = SEARCH_RES.map( item -> {
                return new GCP_BLAST_RID_SCORE( item.req.req_id, item.max_score, item.req.top_n );
            }).cache();

            JavaDStream< GCP_BLAST_RID_SCORE > R_SCORES = SCORES.reduce( ( accum, item ) -> {
                return new GCP_BLAST_RID_SCORE( accum, item );
            });
            /*
            JavaDStream< GCP_BLAST_HSP_JOB > SEARCH_RES2 = SEARCH_RES.reduce( ( accum, item ) -> {
                accum.add( item );
                return accum;
            } ).cache();
            */

            // ***** STEPX:  *****
            SEARCH_RES .foreachRDD( rdd -> {
                long count = rdd.count();
                if ( count > 0 )
                {
                    if ( LOG_FINAL.getValue() )
                    {
                        rdd.foreachPartition( iter -> {
                            int i = 0;
                            while( iter.hasNext() )
                                GCP_BLAST_SEND.send( LOG_HOST.getValue(), LOG_PORT.getValue(),
                                                 String.format( "[%d of %d] %s", i++, count, iter.next().toString() ) );
                        } );
                    }
                    GCP_BLAST_SEND.send( LOG_HOST.getValue(), LOG_PORT.getValue(), String.format( "REQUEST DONE: %d (%d)", count, rdd.id() ) );
                    rdd.saveAsTextFile( String.format( "%s%d", SAVE_DIR.getValue(), rdd.id() ) );
                }
            } );

           jssc.start();               // Start the computation
           System.out.println( "database size: " + numbases.toString() );
           System.out.println( "driver started..." );

           jssc.awaitTermination();    // Wait for the computation to terminate
        }
        catch ( Exception e )
        {
            System.out.println( "stream_version() exception: " + e );
        }
    }

    @Override public void run()
    {
        try
        {
            SparkConf conf = new SparkConf();
            conf.setAppName( settings.appName );
            conf.set( "spark.streaming.stopGracefullyOnShutdown", "true" );

            sc = new JavaSparkContext( conf );
            sc.setLogLevel( "ERROR" );

            // send the given files to all nodes
            for ( String a_file : settings.files_to_transfer )
                sc.addFile( a_file );

            // create a streaming-context from SparkContext given
            jssc = new JavaStreamingContext( sc, Durations.seconds( settings.batch_duration ) );

            stream_version();
        }
        catch ( Exception e )
        {
            System.out.println( "SparkConf exception: " + e );
        }
    }
}
