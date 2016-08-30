#pragma once

#include <map>

#include <ssl_utils.h>
#include <group_ecc_ssl.h>

#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/obj_mac.h>

#define OSSL_CURVE(__NAME__,__ID__)                                         \
        class __NAME__ : public EccSsl                                      \
        {                                                                   \
        public:                                                             \
                __NAME__ (void) :                                           \
                        EccSsl( EC_GROUP_new_by_curve_name( __ID__ ) ) {}   \
        };

namespace psicrypto
{

const static std::string &ecc224_p = 
        "26959946667150639794667015087019630673557916260026308143510066298881";
const static std::string &ecc224_a = 
        "26959946667150639794667015087019630673557916260026308143510066298878";
const static std::string &ecc224_b = 
        "18958286285566608000408668544493926415504680968679321075787234672564";

const static std::string &ecc224_gx=
        "19277929113566293071110308034699488026831934219452440156649784352033";
    
const static std::string &ecc224_gy=
        "19926808758034470970197974370888749184205991990603949537637343198772";
    
const static std::string &ecc224_n=
        "26959946667150639794667015087019625940457807714424391721682722368061";

OSSL_CURVE(EccSslNist521, NID_secp521r1)

OSSL_CURVE(EccSslNist384, NID_secp384r1)

OSSL_CURVE(EccSslNist256, NID_secp256k1)

OSSL_CURVE(EccSslNist224, NID_secp224r1)

OSSL_CURVE(EccSslNist192, NID_secp192k1)

OSSL_CURVE(EccSslNist160, NID_secp160r1)

EC_GROUP *get_ec_group(const std::string id)
{
        EC_GROUP *g=NULL;

        // poor but works
        if(id == "160")
                g = EC_GROUP_new_by_curve_name(NID_secp160r1);
        else if(id == "192")
                g = EC_GROUP_new_by_curve_name(NID_secp192k1);
        else if(id == "224")
                g = EC_GROUP_new_by_curve_name(NID_secp224r1);
        else if(id == "256")
                g = EC_GROUP_new_by_curve_name(NID_secp256k1);
        else if(id == "384")
                g = EC_GROUP_new_by_curve_name(NID_secp384r1);
        else if(id == "521")
                g = EC_GROUP_new_by_curve_name(NID_secp521r1);

        return g;
}

};

#undef OSSL_CURVE

