#pragma once

#include <string>
#include <algorithm>

#include <NTL/ZZ.h>
#include <NTL/ZZVec.h>

#include <openssl/sha.h>

#include <group_abstract.h>
#include <utils.h>

namespace psicrypto
{

class DP_ZZ : public AbstractGroup<NTL::ZZ, NTL::ZZ>
{  

public:
        const NTL::ZZ q, p, g, t;
        const size_t pbits = NumBits(p);
        const size_t qbits = NumBits(q);

protected:
        const size_t pbytes = (pbits-1)/8+1;
        const size_t qbytes = (qbits-1)/8+1;
        const long pzzsize = bytes_to_zzigits(qbytes);

public:
    
        struct Pars
        {
                NTL::ZZ q,p,g;

                static void save(std::ostream &f, const Pars &pars)
                {
        	        f << pars.p << std::endl;
	                f << pars.q << std::endl;
                	f << pars.g << std::endl;
                }

                static void load(std::istream &f, Pars &pars)
                {
                        f >> pars.p;
                        f >> pars.q;
                        f >> pars.g;
                }

                static Pars load(std::istream &f)
                {
                        Pars s;
                        load(f, s);
                        return s;
                }
        };


        DP_ZZ(const Pars &s);

        DP_ZZ(const NTL::ZZ &p, const NTL::ZZ &q, const NTL::ZZ &g,
              const NTL::ZZ &t);

        DP_ZZ(const NTL::ZZ &p, const NTL::ZZ &q, const NTL::ZZ &g);

        void rand(NTL::ZZ &r) const;
            
        void mul(NTL::ZZ &r, const NTL::ZZ &v1, const NTL::ZZ &v2) const;
        NTL::ZZ mul(const NTL::ZZ &v1, const NTL::ZZ &v2) const;

        void gpower(NTL::ZZ &r, const NTL::ZZ &exp) const;
        NTL::ZZ gpower(const NTL::ZZ &exp) const;

        void power(NTL::ZZ &r, const NTL::ZZ &base, const NTL::ZZ &exp) const;

        NTL::ZZ power(const NTL::ZZ &base, const NTL::ZZ &exp) const;        
          
        void hn(std::string &r, const NTL::ZZ &v) const;

        void hp(NTL::ZZ &r, const std::string &v) const;
  
        void h(NTL::ZZ &r, const std::string &v) const;
            
        void inv_exp(NTL::ZZ &r, const NTL::ZZ &v) const;
        NTL::ZZ inv_exp(const NTL::ZZ &v) const;
};

};
