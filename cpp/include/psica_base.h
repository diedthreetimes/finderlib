#pragma once

#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>

#include <utils.h>

namespace psicrypto
{

//////////////////////////////////////////////////////////////////////////////
// Template classes to manage crypto keys and messages of the PSI-CA protocol
//////////////////////////////////////////////////////////////////////////////

template<typename _Exp>
struct PSICA_Key
{

_Exp r1;
_Exp r2;

static void save(std::ostream &f, const PSICA_Key<_Exp> &key)
{
        f << key.r1 << std::endl;
        f << key.r2 << std::endl;
};

static void load(std::istream &f, PSICA_Key<_Exp> &key)
{
        f >> key.r1;
        f >> key.r2;
};

static PSICA_Key<_Exp> load(std::istream &f)
{
        PSICA_Key<_Exp> k;
        load(f,k);
        return k;    
};

};

template<typename _Element>
struct PSICA_Client_Request
{
        _Element X;
        std::vector<_Element> av;
        
        PSICA_Client_Request(const _Element &X) : X(X)
        {
        }
};

template<typename _Element>
struct PSICA_Server_CTX
{
        std::vector<_Element> bhw;
};

template<typename _Element>
struct PSICA_Server_Response
{
        _Element Y;
        std::vector<_Element> apv;
        std::vector<std::string> tsw;

        PSICA_Server_Response(const _Element &Y) :Y(Y)
        {
        }
};

typedef PSICA_Client_Request<std::string> PSICA_Client_Std_Request;
typedef PSICA_Server_Response<std::string> PSICA_Server_Std_Response;

///////////////////////////////////////////////////////////////////////
/// Client and server template classes per PSI-CA protocol
///////////////////////////////////////////////////////////////////////

template<class _FP>
class PSICA_Client
{
public:

typedef _FP                             GROUP;
typedef typename _FP::element_type      ELEMENT;
typedef typename _FP::exp_type          EXP;
typedef PSICA_Client_Request<ELEMENT>   REQUEST;
typedef PSICA_Server_Response<ELEMENT>  RESPONSE;
typedef PSICA_Key<EXP>                  KEY;

typedef std::vector<ELEMENT>            ELEMENT_VECTOR;

protected:

_FP &group;

public:

const EXP rc, rcp;
const EXP rcpinv = group.inv(rcp);
const ELEMENT X = group.gpower(rc);
        
// The constructor does not copy the group!
PSICA_Client(_FP &group, const KEY &key)
        : group(group), rc(key.r1), rcp(key.r2)
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

const REQUEST proc_elements_mp(const std::vector<std::string> &pv)
{
        REQUEST req(this->X);

        ELEMENT_VECTOR &av = req.av; 

        av.resize(pv.size(), group.default_element);
        
        #pragma omp parallel for
        for(uint i=0; i<pv.size(); i++)
        {
                group.hp(av[i], pv[i]);
                group.power(av[i], av[i], rcp);
        }

        return req;
}

const std::vector<std::string> proc_answers_mp(const RESPONSE &r)
{    
        ELEMENT yrc(group.default_element);
        ELEMENT_VECTOR bcv(r.apv.size(), group.default_element);
        //TODO: make it possible to know the target size of the hash function hn
        //      to pre-allocate memory
        std::vector<std::string> hbcv(r.apv.size());

        group.power(yrc, r.Y, rc);
        //TODO: add "power_and_multiply" method to groups?        
        #pragma omp parallel for
        for(uint i=0; i<r.apv.size(); i++)
        {
                group.power(bcv[i], r.apv[i], rcpinv);
                group.mul(bcv[i], bcv[i], yrc);
                group.hn(hbcv[i], bcv[i]);
        }
        
        return hbcv;
}

};

template<class _FP>
class PSICA_Server
{

public:

typedef _FP                             GROUP;
typedef typename _FP::element_type      ELEMENT;
typedef typename _FP::exp_type          EXP;
typedef PSICA_Server_CTX<ELEMENT>       CONTEXT;
typedef PSICA_Client_Request<ELEMENT>   REQUEST;
typedef PSICA_Server_Response<ELEMENT>  RESPONSE;
typedef PSICA_Key<EXP>                  KEY;

typedef std::vector<ELEMENT>            ELEMENT_VECTOR;

protected:
_FP &group;
ELEMENT_VECTOR hset;

public:

PSICA_Server(_FP &group) : group(group)
{
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

const void update(const std::vector<std::string> &set)
{
        hset.resize(set.size(), group.default_element);
        #pragma omp parallel for
        for(size_t i=0; i<set.size(); ++i)
        {
                group.hp(hset[i], set[i]);
        }
}

// generate a new random key and return a server CONTEXT wrt to the actual
// server dataset @hset
const CONTEXT build_context(void)
{
        return build_context(keygen(group));
}

// return a server CONTEXT wrt to the given @key actual server dataset @hset
const CONTEXT build_context( const KEY &key)
{
        CONTEXT ctx;

        ELEMENT_VECTOR &bhw = ctx.bhw;

        bhw.resize(hset.size(), group.default_element);

        #pragma omp parallel for
        for(size_t i=0; i<hset.size(); ++i)
        {
                group.power(bhw[i], hset[i], key.r2);
        }
        std::random_shuffle(bhw.begin(), bhw.end());

        return ctx;
}

// process data from a client with no pre-known server key and pre-computation
const void proc_elements_mp( RESPONSE &resp, const REQUEST &req)
{

        return proc_elements_mp(resp, req, keygen(group));
}

// process data from a client using known @key, but no pre-computation
const void proc_elements_mp( RESPONSE &resp, const REQUEST &req,
                             const KEY &key)
{
        return proc_elements_mp(resp, req, key, build_context(key));
}

// process data from a client using known server @CONTEXT
const void proc_elements_mp( RESPONSE &resp, const REQUEST &req,
                             const KEY &key, const CONTEXT &ctx)
{
        // Computing DH shared secret using client X and key rs
        const ELEMENT &xrs = group.power(req.X, key.r1);

        group.gpower(resp.Y, key.r1);

        // the next two blocks of operations are independent.
        // Currently execution is sequential but each block of operation is parallel

        resp.apv.resize(req.av.size(), group.default_element);

        //// First block of operations
        #pragma omp parallel for
        for(size_t i=0; i<req.av.size() ; ++i)
        {
                group.power(resp.apv[i], req.av[i], key.r2);
        }
        std::random_shuffle(resp.apv.begin(), resp.apv.end());

        ///// Second block of operations, currently implemented as first parallel 
        ELEMENT_VECTOR bv(ctx.bhw.size(), group.default_element);
        resp.tsw.resize(bv.size());
        #pragma omp parallel for
        for(size_t i=0; i<bv.size(); ++i)
        {
                group.mul(bv[i], xrs, ctx.bhw[i]);
                group.hn(resp.tsw[i], bv[i]);
        }
}

const RESPONSE proc_elements_mp( const REQUEST &req, const KEY &key, const CONTEXT &ctx)
{
        RESPONSE resp(group.default_element);
        proc_elements_mp(resp, req, key, ctx);
        return resp;
}

const RESPONSE proc_elements_mp( const REQUEST &req, const KEY &key)
{
        RESPONSE resp(group.default_element);
        proc_elements_mp(resp, req, key);
        return resp;
}


};


};

