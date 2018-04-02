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
import java.util.Collections;
import java.io.Serializable;

class GCP_BLAST_RID_SCORE implements Serializable
{
    String rid;
    Integer score;
    Integer top_n;
    List< Integer > top_scores;

    public GCP_BLAST_RID_SCORE()
    {
        rid = "";
        score = 0;
        top_n = 0;
        top_scores = new ArrayList<>();
    }

    public GCP_BLAST_RID_SCORE( final GCP_BLAST_RID_SCORE other, final GCP_BLAST_RID_SCORE item )
    {
        rid = other.rid;
        score = other.score;
        top_n = other.top_n;
        top_scores = new ArrayList<>();
        top_scores.addAll( other.top_scores );
        add( item );
    }

    public GCP_BLAST_RID_SCORE( final String rid, final Integer score, final Integer top_n )
    {
        this.rid = rid;
        this.score = score;
        this.top_n = top_n;
        top_scores = new ArrayList<>();
    }
   
    public void add( final GCP_BLAST_RID_SCORE item )
    {
        if ( rid.isEmpty() )
            rid = item.rid;
        if ( top_n == 0 )
            top_n = item.top_n;
        if ( top_scores.size() < top_n )
        {
            top_scores.add( item.score );
            Collections.sort( top_scores );
        }
        else
        {
            if ( item.score > top_scores.get( 0 ) )
            {
                top_scores.set( 0, item.score );
                Collections.sort( top_scores );
            }
        }
    }

    @Override public String toString()
    {
        return String.format( "rid: %s, score: %d", rid, score );
    }
}

