#include <group_ecc_ssl.h>

//https://www.openssl.org/docs/crypto/EC_POINT_new.html

//https://www.openssl.org/docs/crypto/EC_GROUP_new.html

#include <string>
#include <sstream>
#include <vector>
#include <iostream>
#include <algorithm>
#include <assert.h>

#include <openssl/sha.h>
#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/rand.h>
#include <openssl/obj_mac.h>
//#include <openssl/err.h>

#include <utils.h>
#include <ssl_utils.h>
#include <constants.h>
#include <group_abstract.h>

using namespace psicrypto;
typedef unsigned char uchar;

EccSsl::EccSsl(const EC_GROUP *g) : AbstractGroup(WECP(g))
{

        this->g = EC_GROUP_dup(g);

        assert(this->g);
        
        WBNCTX ctx;
        EC_GROUP_precompute_mult(this->g, ctx.p);

        EC_GROUP_get_order((const EC_GROUP*)this->g, this->n.p, NULL);
        this->nbytes = (size_t)BN_num_bytes(this->n.p);
}
    
// making sure that no copy or copy constructor is called
// without being noticed. Any copy shouldn't rely on automatic
// c++ management but on custom openssl routines (TODO)
EccSsl::EccSsl(const EccSsl &rhs) : AbstractGroup(WECP(rhs.g))
{
        assert(0);
}
    
EccSsl& EccSsl::operator=(const EccSsl &rhs)
{
        assert(0);
}
    
EccSsl::~EccSsl()
{
//        BN_CTX_end(ctx);
//        BN_CTX_free(ctx);
        EC_GROUP_free(g);
}

WBN& EccSsl::rand(WBN &r) const
{
        // TODO: check if pseudo-random and if it needs explicit seeding
        // https://www.openssl.org/docs/crypto/RAND_bytes.html
        // https://www.openssl.org/docs/crypto/BN_rand.html
        // Test r.p size > q-1 bits
        BN_rand_range(r.p, this->n.p);
        return r;
};

std::vector<WECP>& EccSsl::shuffle(std::vector<WECP> &v) const
{
        // TODO: init random see or/and use an openssl random generator
        // TODO: design the shuffle as an iterator
        std::random_shuffle(v.begin(), v.end());
        return v;
};
    
WECP& EccSsl::mul(WECP &r, const WECP &v, const WECP &c)
{
        thread_local WBNCTX ctx;

        BN_CTX_start(ctx.p);        
        EC_POINT_add((const EC_GROUP*)this->g, r.p, v.p, c.p, ctx.p);
        BN_CTX_end(ctx.p);
        return r;
}

WECP EccSsl::mul(const WECP &v, const WECP &c)
{
        WECP r(g);
        mul(r, v, c);
        return r;
}

WECP& EccSsl::gpower(WECP &r, const WBN &exp) const
{
        thread_local WBNCTX ctx;

        BN_CTX_start(ctx.p);

        EC_POINT_mul((const EC_GROUP*)this->g, r.p, exp.p, NULL, NULL , ctx.p);

        BN_CTX_end(ctx.p);

        return r;
}

WECP EccSsl::gpower(const WBN &exp) const
{
        WECP r(g);
        gpower(r, exp);
        return r;
}

WECP& EccSsl::power(WECP &r, const WECP &base, const WBN &exp) const
{
        thread_local WBNCTX ctx;

        BN_CTX_start(ctx.p);        

        EC_POINT_mul((const EC_GROUP*)this->g, r.p, NULL, base.p, exp.p,ctx.p);

        BN_CTX_end(ctx.p);
        return r;
}
    
WECP EccSsl::power(const WECP &base, const WBN &exp) const
{
        WECP r(g);
        power(r, base, exp);
        return r;
}

std::string& EccSsl::hn(std::string &r, const WECP &v) const
{
        // https://www.openssl.org/docs/crypto/sha.html
        const static size_t max_size = std::min(this->nbytes,
                                                (size_t)SHA512_DIGEST_LENGTH);
        thread_local unsigned char buff[SHA512_DIGEST_LENGTH];
        std::stringstream ss;
        ss << v;
        const std::string &s = ss.str();
        
        SHA512((const unsigned char*)&s[0], s.size(), buff);
        r.assign((const char*)buff, max_size);
        return r;
}

std::string EccSsl::hn(const WECP &v) const
{
        std::string r;
        return hn(r, v);
}

WECP& EccSsl::hp(WECP &r, const std::string &s) const
{
        // fdh versions?
        // https://eprint.iacr.org/2009/226.pdf 
        // https://eprint.iacr.org/2009/340.pdf
        const static size_t max_size = std::min(this->nbytes, 
                                         (const size_t)SHA512_DIGEST_LENGTH);
        const static uchar MAX_TRY = 100;
        thread_local SHA512_CTX sha_ctx;
        thread_local unsigned char buff[SHA512_DIGEST_LENGTH];
        thread_local WBN v;
        thread_local WBNCTX ctx;

        BN_CTX_start(ctx.p);        
        uint found=0;
        
        for( uchar i=0 ; i < MAX_TRY && !found ; i++ )
        {
                SHA512_Init(&sha_ctx);
                SHA512_Update(&sha_ctx, &i, 1);
                SHA512_Update(&sha_ctx, (const unsigned char*)&s[0], s.size());
                SHA512_Final(buff, &sha_ctx);
                // TODO: hash to uniform BIGNUM Zn to be changed
                BN_bin2bn(buff, max_size, v.p);
                BN_mod(v.p, v.p, this->n.p, ctx.p);
                found = EC_POINT_set_compressed_coordinates_GFp( 
                        (const EC_GROUP*)this->g, r.p, 
                        v.p, i%2, ctx.p);
        }
        BN_CTX_end(ctx.p);
        assert(found);
        return r;
}

WECP EccSsl::hp(const std::string &s) const
{
        WECP r(g);
        hp(r, s);
        return r;
}
    
WBN& EccSsl::inv(WBN &r, const WBN &v)
{
        thread_local WBNCTX ctx;
        BN_CTX_start(ctx.p);        
        BN_mod_inverse(r.p, v.p, n.p, ctx.p);
        BN_CTX_end(ctx.p);
        return r;
}

WBN EccSsl::inv(const WBN &v)
{
        WBN r;
        inv(r, v);
        return r;
}



