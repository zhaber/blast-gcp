/*  $Id$
 * ===========================================================================
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
 * Author:  Christiam Camacho, Greg Boratyn
 *
 */

/// @file blast4spark.hpp
/// Funtion prototypes to call BLAST from Spark via JNI

#ifndef ALGO_BLAST_API___BLAST4SPARK__HPP
#define ALGO_BLAST_API___BLAST4SPARK__HPP

#include <corelib/ncbistd.hpp>
#include <algo/blast/core/blast_export.h>
#include <algo/blast/core/blast_hspstream.h>

/** @addtogroup AlgoBlast
 *
 * @{
 */

BEGIN_NCBI_SCOPE
BEGIN_SCOPE(blast)

NCBI_XBLAST_EXPORT 
BlastHSPStream*
PrelimSearch(const std::string& single_query, 
        const std::string& database_name,
        const std::string& program_name);

END_SCOPE(blast)
END_NCBI_SCOPE

/* @} */

#endif  /* ALGO_BLAST_API___BLAST4SPARK__HPP */
