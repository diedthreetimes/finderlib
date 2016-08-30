#pragma once

//https://www.openssl.org/docs/crypto/EC_POINT_new.html

//https://www.openssl.org/docs/crypto/EC_GROUP_new.html

#include <string>
#include <sstream>
#include <vector>
#include <iostream>
#include <assert.h>

#include <ssl_utils.h>
#include <group_abstract.h>

namespace psicrypto
{

//template<const EC_GROUP* G>
class EccSsl : public AbstractGroup<WECP, WBN>
{   
protected:
//        BN_CTX *ctx = NULL;
        EC_GROUP *g = NULL;
        const WBN n;
        size_t nbytes;
public:
        EccSsl(const EC_GROUP *g);
        EccSsl(const EccSsl &rhs);        
        EccSsl &operator=(const EccSsl &rhs);    
        ~EccSsl();    
        WBN& rand(WBN &r) const;
        std::vector<WECP>& shuffle(std::vector<WECP> &v) const;    
        WECP& mul(WECP &r, const WECP &v, const WECP &c);    
        WECP mul(const WECP &v, const WECP &c);  
        WECP& gpower(WECP &r, const WBN &exp) const;    
        WECP gpower(const WBN &exp) const;
        WECP& power(WECP &r, const WECP &base, const WBN &exp) const;    
        WECP power(const WECP &base, const WBN &exp) const;
        std::string& hn(std::string &r, const WECP &v) const;
        std::string hn(const WECP &v) const;  
        WECP& hp(WECP &r, const std::string &s) const;        
        WECP hp(const std::string &s) const;    
        WBN& inv(WBN &r, const WBN &v);
        WBN inv(const WBN &v);
};

};




