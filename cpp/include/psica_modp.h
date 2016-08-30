#pragma once

#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>

#include <psica_base.h>

namespace psicrypto
{

////////////////////////////////////////////////////////////////////////////////
// Optimized psica client for integer fields
////////////////////////////////////////////////////////////////////////////////

template<class _FP>
class PSICA_MODP_Client
{

public:

typedef _FP                            GROUP;
typedef typename GROUP::element_type   ELEMENT;
typedef typename GROUP::exp_type       EXP;
typedef PSICA_Client_Request<ELEMENT>  REQUEST;
typedef PSICA_Server_Response<ELEMENT> RESPONSE;
typedef PSICA_Key<EXP>                 KEY;

// Actual key of the client
const EXP rc, rcp;

protected:

_FP &group;

// Cached values
// DH X element
const ELEMENT X = group.gpower(rc);
// inverse modq of rcp
const EXP rcpinv = group.inv_exp(rcp);
// Cached exponent for FDH of set items
const ELEMENT h_exp = group.mul(group.t, rcp);

public:
        
PSICA_MODP_Client(_FP &group, const KEY &key)
            :   rc(key.r1),
                rcp(key.r2),
                group(group)
{
        // TODO: some assertions?
}

static KEY keygen(const _FP &group)
{
        KEY k;
        group.rand(k.r1);
        do{
                group.rand(k.r2);
        }while(k.r1 == k.r2);
        return k;
}


const REQUEST proc_elements_mp(
        const std::vector<std::string> &pv)
{

        // Computing X
        const auto &X = group.gpower(rc);
        REQUEST req(X);

        // Computing av
        std::vector<ELEMENT> &av = req.av;
        av.resize(pv.size(), group.default_element);

        #pragma omp parallel for
        for(size_t i=0; i < av.size() ; ++i)
        {
                group.h(av[i], pv[i]);
                group.power(av[i], av[i], h_exp);
        }

        return req;
}

const std::vector<std::string> proc_answers_mp(const RESPONSE &r)
{    
        const ELEMENT &yrc = group.power(r.Y, rc);

        //TODO: add "power_and_multiply" method to groups
        std::vector<ELEMENT> v(r.apv.size());
        std::vector<std::string> s(r.apv.size());
        
        #pragma omp parallel for
        for(size_t i=0; i<v.size(); ++i)
        {
                group.power(v[i], r.apv[i], rcpinv);
                group.mul(v[i], v[i], yrc);
                group.hn(s[i], v[i]);
        }

        return s;
}
};

//template<class _FP>
//class PSICA_MODP_Server
//{

//public:
//        typedef typename _FP::element_type     ELEMENT;
//        typedef typename _FP::exp_type         EXP;
//        typedef PSICA_Server_CTX<_FP>          CONTEXT;
//        typedef PSICA_Client_Request<_FP>      REQUEST;
//        typedef PSICA_Server_Response<_FP>     RESPONSE;
//        typedef PSICA_Key<_FP>                 KEY;

//protected:
//        _FP &group;
//        std::vector<ELEMENT> hset;
//public:

//        PSICA_MODP_Server(_FP &group) : group(group)
//        {
//        }

//        static KEY keygen(const _FP &group)
//        {
//                KEY k;
//                group.rand(k.r1);
//                do{
//                        group.rand(k.r2);
//                }while(k.r1 == k.r2);
//                return k;
//        }

//        const void update(const std::vector<std::string> &set)
//        {
//                hset.resize(set.size());
//                #pragma omp parallel for
//                for(size_t i=0; i<set.size(); ++i)
//                {
//                        group.hp(hset[i], set[i]);
//                }
//        }

//        // generate a new random key and return a server CONTEXT wrt to the actual
//        // server dataset @hset
//        const CONTEXT build_context(void)
//        {
//                return build_context(keygen(group));
//        }

//        // return a server CONTEXT wrt to the given @key actual server dataset @hset
//        const CONTEXT build_context( const KEY &key)
//        {
//                CONTEXT ctx;

//                std::vector<ELEMENT> &bhw = ctx.bhw;

//                bhw.resize(hset.size());

//                #pragma omp parallel for
//                for(size_t i=0; i<hset.size(); ++i)
//                {
//                        group.power(bhw[i], hset[i], key.r2);
//                }
//                std::random_shuffle(bhw.begin(),bhw.end());

//                return ctx;
//        }

//        // process data from a client with no pre-known server key and pre-computation
//        const void proc_elements_mp( RESPONSE &resp, const REQUEST &req)
//        {

//                return proc_elements_mp(resp, req, keygen(group));
//        }

//        // process data from a client using known @key, but no pre-computation
//        const void proc_elements_mp( RESPONSE &resp, const REQUEST &req,
//                                     const KEY &key)
//        {
//                return proc_elements_mp(resp, req, key, build_context(key));
//        }

//        // process data from a client using known server @CONTEXT
//        const void proc_elements_mp( RESPONSE &resp, const REQUEST &req,
//                                     const KEY &key, const CONTEXT &ctx)
//        {
//                // Computing DH shared secret using client X and key rs

//                const ELEMENT &xrs = group.power(req.X, key.r1);

//                group.gpower(resp.Y, key.r1);

//                // the next two blocks of operations are independent.
//                // Currently execution is sequential but each block of operation is parallel

//                resp.apv.resize(req.av.size());

//                //// First block of operations
//                #pragma omp parallel for
//                for(size_t i=0; i<req.av.size() ; ++i)
//                {
//                        group.power(resp.apv[i], req.av[i], key.r2);
//                }
//                std::random_shuffle(resp.apv.begin(), resp.apv.end());

//                ///// Second block of operations, currently implemented as first parallel 
//                std::vector<ELEMENT> bv(ctx.bhw.size());
//                resp.tsw.resize(bv.size());
//                #pragma omp parallel for
//                for(size_t i=0; i<bv.size(); ++i)
//                {
//                        group.mul(bv[i], xrs, ctx.bhw[i]);
//                        group.hn(resp.tsw[i], bv[i]);
//                }
//        }

//};

};

