#include <group_dp_zz.h>

#include <string>
#include <algorithm>

#include <openssl/sha.h>
#include <openssl/rand.h>
#include <group_abstract.h>

psicrypto::DP_ZZ::DP_ZZ(const Pars &s) : DP_ZZ(s.p, s.q, s.g)
{
}

psicrypto::DP_ZZ::DP_ZZ(const NTL::ZZ &p, const NTL::ZZ &q, const NTL::ZZ &g)
        : DP_ZZ(p, q, g, (p-1)/q)
{
}

psicrypto::DP_ZZ::DP_ZZ(const NTL::ZZ &p, const NTL::ZZ &q, const NTL::ZZ &g,
                        const NTL::ZZ &t)
        : AbstractGroup(NTL::ZZ::zero()), q(q), p(p), g(g), t(t)
{
}

void psicrypto::DP_ZZ::rand(NTL::ZZ &r) const
{
        do{
                NTL::GenPrime(r, qbits);
        }while(r >= q);
}

void psicrypto::DP_ZZ::gpower(NTL::ZZ &r, const NTL::ZZ &exp) const
{
        NTL::PowerMod(r, g, exp, p);
}

NTL::ZZ psicrypto::DP_ZZ::gpower(const NTL::ZZ &exp) const
{
        NTL::ZZ r;
        gpower(r,exp);
        return r;
}


void psicrypto::DP_ZZ::power(NTL::ZZ &r, const NTL::ZZ &base, const NTL::ZZ &exp) const
{
    // not asserting if base is an element of the subgroup    
        NTL::PowerMod(r, base, exp, p);
}

NTL::ZZ psicrypto::DP_ZZ::power(const NTL::ZZ &base, const NTL::ZZ &exp) const
{
        NTL::ZZ r;
        power(r,base, exp);
        return r;
}


void psicrypto::DP_ZZ::mul(NTL::ZZ &r, const NTL::ZZ &v, const NTL::ZZ &c) const
{
        NTL::MulMod(r, v, c, p);
}

NTL::ZZ psicrypto::DP_ZZ::mul(const NTL::ZZ &v, const NTL::ZZ &c) const
{
        NTL::ZZ r;
        mul(r, v, c);
        return r;
}



void psicrypto::DP_ZZ::hn(std::string &r, const NTL::ZZ &v) const
{
        thread_local static std::vector<unsigned char> buff(
                std::max(pbytes, (size_t) SHA512_DIGEST_LENGTH));
        thread_local static SHA512_CTX context;

        NTL::BytesFromZZ(&buff[0], v, pbytes);

        SHA512_Init(&context);
        SHA512_Update(&context, &buff[0], pbytes);
        SHA512_Final(&buff[0], &context); // save hash to buffer and clear context

        r.assign((char *)&buff[0], qbytes);
}

void psicrypto::DP_ZZ::hp(NTL::ZZ &r, const std::string &v) const
{
        this->h(r, v);
        NTL::PowerMod(r, r, t, p);
}

void psicrypto::DP_ZZ::h(NTL::ZZ &r, const std::string &v) const
{  
        thread_local static unsigned char buff[SHA512_DIGEST_LENGTH];
        thread_local static SHA512_CTX context;

        SHA512_Init(&context);
        SHA512_Update(&context, &v[0], v.size());
        SHA512_Final(buff, &context); // save hash to buffer and clear context
        ZZFromBytes(r, buff, qbytes);
}


void psicrypto::DP_ZZ::inv_exp(NTL::ZZ &r, const NTL::ZZ &v) const
{
        NTL::InvMod(r, v, q);
}

NTL::ZZ psicrypto::DP_ZZ::inv_exp(const NTL::ZZ &v) const
{
        NTL::ZZ r;
        inv_exp(r, v);
        return r;
}
