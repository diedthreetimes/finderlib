#pragma once

#include <NTL/ZZ.h>

#include <ssl_utils.h>

namespace psicrypto
{

///////////////////////////////////////////////////////////////////////
/// Useful constants
///////////////////////////////////////////////////////////////////////

// Using read only functions for NTL
// see http://www.shoup.net/ntl/doc/tour-struct.html
inline const NTL::ZZ& zz_one(void)
{
        static NTL::ZZ one = NTL::conv<NTL::ZZ>("1");
        return one;
}

inline const NTL::ZZ& zz_two(void)
{
        static NTL::ZZ two = NTL::conv<NTL::ZZ>("2");
        return two;
}

};
