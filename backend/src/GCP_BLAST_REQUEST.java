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

import java.io.*;

class GCP_BLAST_REQUEST implements Serializable
{
    public final String job_id, db, query, params;
    
    public GCP_BLAST_REQUEST( final String line )
    {
        String[] parts = line.split( "\\:" );
        this.job_id = ( parts.length > 0 ) ? parts[ 0 ] : "dflt_job_id";
        this.db     = ( parts.length > 1 ) ? parts[ 1 ] : "dflt_db";
        this.query  = ( parts.length > 2 ) ? parts[ 2 ] : "dflt_query";
        this.params = ( parts.length > 3 ) ? parts[ 3 ] : "dflt_params";
    }
    
    @Override public String toString()
    {
        return String.format( "%s.%s.%s.%s", this.job_id, this.db, this.query, this.params );
    }
}
