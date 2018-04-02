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
import java.io.Serializable;

class GCP_BLAST_HSP_JOB implements Serializable
{
    List< GCP_BLAST_HSP_LIST > hspl;
    
    public GCP_BLAST_HSP_JOB()
    {
        this.hspl = new ArrayList<>();
    }
   
    public void add( final GCP_BLAST_HSP_LIST item )
    {
        int n = hspl.size();
        if ( n > 0 )
        {
            if ( !item.isEmpty() )
                hspl.add( item );
        }
        else
        {
            hspl.add( item );
        }
    }

    @Override public String toString()
    {
        String res = String.format( "HSPJOB( %d )\n", hspl.size() );
        for ( GCP_BLAST_HSP_LIST item : hspl )
            res = res + String.format( "%s\n", item.toString() );
        return res;
    }
}
