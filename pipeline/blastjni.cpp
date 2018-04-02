/* ===========================================================================
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

#include "BlastJNI.h"
#include <jni.h>

#include "blast4spark.hpp"
#include <algo/blast/api/blast_advprot_options.hpp>
#include <algo/blast/api/blast_exception.hpp>
#include <algo/blast/api/blast_nucl_options.hpp>
#include <algo/blast/api/blast_results.hpp>
#include <algo/blast/api/local_blast.hpp>
#include <algo/blast/api/objmgrfree_query_data.hpp>
#include <algo/blast/api/prelim_stage.hpp>
#include <algo/blast/api/setup_factory.hpp>
#include <algo/blast/core/blast_hspstream.h>
#include <algorithm>
#include <ctype.h>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <ncbi_pch.hpp>
#include <objects/seq/Bioseq.hpp>
#include <objects/seq/Seq_data.hpp>
#include <objects/seqalign/Seq_align.hpp>
#include <objects/seqalign/Seq_align_set.hpp>
#include <objects/seqloc/Seq_id.hpp>
#include <objects/seqset/Bioseq_set.hpp>
#include <objects/seqset/Seq_entry.hpp>

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

#include <sstream>
#include <string>
#include <vector>

static void jni_throw(JNIEnv* jenv, jclass jexcept_cls, const char* fmt,
                      va_list args)
{
    // expand message into buffer
    char msg[4096];
    int size = vsnprintf(msg, sizeof msg, fmt, args);

    // ignore overruns
    if (size < 0 || (size_t)size >= sizeof msg)
        strcpy(&msg[sizeof msg - 4], "...");

    // create error object, put JVM thread into Exception state
    jenv->ThrowNew(jexcept_cls, msg);
}

enum
{
    xc_no_err,
    xc_java_exception,

    // should be last
    xc_java_runtime_exception
};

static void jni_throw(JNIEnv* jenv, uint32_t xtype, const char* fmt,
                      va_list args)
{
    jclass jexcept_cls = 0;

    // select exception types
    switch (xtype) {
        case xc_java_exception:
            jexcept_cls = jenv->FindClass("java/lang/Exception");
            break;
    }

    // if not a known type, must throw RuntimeException
    if (jexcept_cls == 0)
        jexcept_cls = jenv->FindClass("java/lang/RuntimeException");

    jni_throw(jenv, jexcept_cls, fmt, args);
}

static void jni_throw(JNIEnv* jenv, uint32_t xtype, const char* fmt, ...)
{
    if (xtype != xc_no_err) {
        va_list args;
        va_start(args, fmt);

        jni_throw(jenv, xtype, fmt, args);

        va_end(args);
    }
}

static void jni_log(JNIEnv* jenv, jobject jthis, jmethodID jlog_method,
                    const char* fmt, ...)
{
    if (jlog_method != 0) {
        va_list args;
        va_start(args, fmt);

        char buffer[4096];
        int size = vsnprintf(buffer, sizeof buffer, fmt, args);

        va_end(args);

        if (size < 0)
            strcpy(buffer,
                   "jni_log: failed to make a String ( bad format or string "
                   "too long )");
        else if ((size_t)size >= sizeof buffer)
            strcpy(buffer,
                   "jni_log: failed to make a String ( string too long )");

        // make String object
        jstring jstr = jenv->NewStringUTF(buffer);

        // call the Java log method
        jenv->CallObjectMethod(jthis, jlog_method, jstr);
    }
}

static jobjectArray iterate_HSPs(jclass GCP_BLAST_HSP_LIST,
                                 std::vector<BlastHSPList*>& hsp_lists)
{
    // we know the number of HSP list objects in the array from hsp_lists .
    // count ().

    // iterate across all hsp_lists
    // for each, walk their hsp_array looking for max score
    // determine the size of a malloc/new allocation for opaque HSP data
    /*
      HSP
      {
         score, qstart, qlen, sstart, slen, qframe, sframe, qgapped_start,
      sgapped_start
      }
     */
    // allocate a GCP_BLAST_HSP_LIST with params - missing params here.
}

static void whack_hsp_lists(std::vector<BlastHSPList*>& hsp_lists)
{
    size_t i, count = hsp_lists.size();
    for (i = 0; i < count; ++i) Blast_HSPListFree(hsp_lists[i]);
}

static jobjectArray prelim_search(JNIEnv* jenv, jobject jthis,
                                  jmethodID jlog_method, const char* query,
                                  const char* db_spec, const char* program,
                                  const char* params)
{
    ncbi::blast::BlastHSPStream* hsp_stream
        = ncbi::blast::PrelimSearch(query, db_spec, sparams);

    if (hsp_stream == 0)
        throw std::exception("prelim_search - NULL hsp_stream");

    jclass GCP_BLAST_HSP_LIST = jenv->FindClass("GCP_BLAST_HSP_LIST");
    if (GCP_BLAST_HSP_LIST == 0)
        throw std::exception(
            "prelim_search - failed to locate GCP_BLAST_HSP_LIST class");

    jobjectArray jhsps = 0;
    std::vector<BlastHSPList*> hsp_lists;

    try {
        while (1) {
            BlastHSPList* hsp_list = 0;
            int status = BlastHSPStreamRead(hsp_stream, &hsp_list);

            if (status == kBlastHSPStream_Error)
                throw std::exception(
                    "prelim_search - Exception from BlastHSPStreamRead");

            if (status != kBlastHSPStream_Success || hsp_list == 0) break;

            hsp_lists.push_back(hsp_list);
        }

        jhsps = iterate_HSPs(GCP_BLAST_HSP_LIST, hsp_lists);
    }
    catch (...) {
        whack_hsp_lists(hsp_lists);
        BlastHSPStreamFree(hsp_stream);
        throw;
    }

    whack_hsp_lists(hsp_lists);
    BlastHSPStreamFree(hsp_stream);
    return jhsps;
}

/*
 * Class:     GCP_BLAST_LIB
 * Method:    prelim_search
 * Signature:
 * (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_GCP_1BLAST_1LIB_prelim_1search(
    JNIEnv* jenv, jobject jthis, jstring jquery, jstring jdb_spec,
    jstring jprogram, jstring jparams, jboolean have_log)
{
    uint32_t xtype = xc_no_err;

    jobjectArray ret = 0;

    const char* query = jenv->GetStringUTFChars(jquery, 0);
    const char* db_spec = jenv->GetStringUTFChars(jdb_spec, 0);
    const char* program = jenv->GetStringUTFChars(jprogram, 0);
    const char* params = jenv->GetStringUTFChars(jparams, 0);

    jmethodID jlog_method = 0;
    if (have_log) {
        jclass GCP_BLAST_LIB = jenv->GetObjectClass(jthis);
        jlog_method = jenv->GetMethodID(GCP_BLAST_LIB, "log",
                                        "(Ljava/lang/String;)V");
    }

    try {
        ret = prelim_search(jenv, jthis, jlog_method, query, db_spec, program,
                            params);
    }
    catch (std::exception& x) {
        jni_throw(jenv, xtype = xc_java_exception, "%s", x.what());
    }
    catch (...) {
        jni_throw(jenv, xtype = xc_java_runtime_exception,
                  "%s - unknown exception", __func__);
    }

    jenv->ReleaseStringUTFChars(jquery, query);
    jenv->ReleaseStringUTFChars(jdb_spec, db_spec);
    jenv->ReleaseStringUTFChars(jprogram, program);
    jenv->ReleaseStringUTFChars(jparams, params);

    return ret;
}

/*
 * Class:     GCP_BLAST_LIB
 * Method:    traceback
 * Signature:
 * (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_GCP_1BLAST_1LIB_traceback(
    JNIEnv* jenv, jobject jthis, jstring jquery, jstring jdb_spec,
    jstring jprogram, jstring jparams, jobjectArray jjsonHSPs)
{
    uint32_t xtype = xc_no_err;

    jobjectArray ret = NULL;

    const char* query = jenv->GetStringUTFChars(jquery, 0);
    const char* db_spec = jenv->GetStringUTFChars(jdb_spec, 0);
    const char* program = jenv->GetStringUTFChars(jprogram, 0);
    const char* params = jenv->GetStringUTFChars(jparams, 0);

    jmethodID jlog_method = 0;
    if (have_log) {
        jclass GCP_BLAST_LIB = jenv->GetObjectClass(jthis);
        jlog_method = jenv->GetMethodID(GCP_BLAST_LIB, "log",
                                        "(Ljava/lang/String;)V");
    }

    try {
        ret = traceback(jenv, jthis, jlog_method, query, db_spec, program,
                        params, hsps);
    }
    catch (std::exception& x) {
        jni_throw(jenv, xtype = xc_java_exception, "%s", x.what());
    }
    catch (...) {
        jni_throw(jenv, xtype = xc_java_runtime_exception,
                  "%s - unknown exception", __func__);
    }

    jenv->ReleaseStringUTFChars(jquery, query);
    jenv->ReleaseStringUTFChars(jdb_spec, db_spec);
    jenv->ReleaseStringUTFChars(jprogram, program);
    jenv->ReleaseStringUTFChars(jparams, params);

    return ret;
}
