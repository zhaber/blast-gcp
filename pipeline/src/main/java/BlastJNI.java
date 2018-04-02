/*
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
 */

import java.lang.String;
import java.lang.Throwable;
import java.lang.ExceptionInInitializerError;
import java.io.PrintWriter;

public class BlastJNI
{
    public String [] jni_prelim_search ( String query, String db_spec, String program, String params )
    {
        return jni . prelim_search ( query, db_spec, program, params );
    }

    public String [] jni_traceback ( String query, String db_spec, String program, String params, final String [] jsonHSPs )
    {
        return jni . traceback ( query, db_spec, program, params, jsonHSPs );
    }

    public BlastJNI ()
    {
        jni . throwIfBad ();
    }

    public static void main ( String [] args )
    {
        /*
           String rid  ="ReqID123";
           String query ="CCGCAAGCCAGAGCAACAGCTCTAACAAGCAGAAATTCTGACCAAACTGATCCGGTAAAACCGATCAACG";
           String db    ="nt";
           String db_bucket=db + "_50mb_chunks";
           String params="blastn";

        // Prelim_search test
        ArrayList<String> al=new ArrayList<String>();
        // Keep it manageble, above query has hits in partitions #14 & 18
        for (int partnum=12; partnum <= 18; ++partnum)
        {
        String part=db + "_50M." + String.format("%02d", partnum);

        log("----   Processing part " + partnum);
        String results[]=new BlastJNI().jni_prelim_search(db_bucket, db, rid, query, part, params);

        log("Java results[] has " + results.length + " entries:");
        log(Arrays.toString(results));
        al.addAll(Arrays.asList(results));
        }
        String hsp[]=al.toArray(new String[0]);

        // Traceback test
        String[] tracebacks=new BlastJNI().jni_traceback(hsp);
        log("Java traceback[] has " + tracebacks.length + " entries:");
        log(Arrays.toString(tracebacks));
        */
    }

    private static Blast_JNI jni = new Blast_JNI ();
}

