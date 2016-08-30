#pragma once

#include <string>
#include <sstream>
#include <iostream>

#include <assert.h>
#include <errno.h>
#include <pthread.h>

#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <openssl/ssl.h>

#include <utils.h>

int ssl_init(void);
int ssl_close(void);

inline unsigned long pthreads_thread_id(void)
{
        unsigned long ret;

        ret= (unsigned long)pthread_self();
        return ret;
}

namespace psicrypto
{

// wrapping bignum context (especially useful for implicit allocation and
// destruction when using thread_local variables...)
class WBNCTX
{
    public:
        BN_CTX *p = NULL;
        
        WBNCTX();
        ~WBNCTX();
};

// wrapping openssl BIGNUM
class WBN
{
public:
        BIGNUM *p=NULL;
        
        WBN();
        WBN(const WBN &v);
        ~WBN();
        WBN &operator=(const WBN &rhs);
        bool operator==(const WBN &rhs) const;
};

// wrapping openssl EC_POINT
class WECP
{
protected:
        const EC_GROUP *g = NULL;
public:
        EC_POINT *p = NULL;
        
        WECP(const EC_GROUP *g);
        WECP(const WECP &rhs);
        ~WECP();
        WECP &operator=(const WECP &rhs);
        bool operator==(const WECP &rhs) const;
        std::string to_str() const;
        void from_str(const std::string&);
};

std::ostream& operator<<(std::ostream &os, const psicrypto::WBN &obj);

std::istream& operator>>(std::istream &is, psicrypto::WBN &obj);

std::ostream& operator<<(std::ostream &os, const psicrypto::WECP &obj);

std::istream& operator>>(std::istream &is, psicrypto::WECP &obj);

};

namespace std
{

template<>
struct hash<psicrypto::WBN> {
        std::size_t operator()(const psicrypto::WBN &v) const
        {
                std::stringstream ss;
                ss << v;
                return std::hash<std::string>()(ss.str());
        }
};

template<>
struct hash<psicrypto::WECP> {
        std::size_t operator()(const psicrypto::WECP &v) const
        {
                std::stringstream ss;
                ss << v;
                return std::hash<std::string>()(ss.str());
        }
};

};


