#pragma once

#include <string>
#include <sstream>

#include <NTL/ZZ.h>

inline std::string
StringFromZZ(const NTL::ZZ &x)
{
        std::string s;
        s.resize(NumBytes(x), 0);
        NTL::BytesFromZZ((uint8_t*) &s[0], x, s.length());
        return s;
}

inline std::string
StringFromZZ(const NTL::ZZ &x, size_t pad_size)
{
        std::string s;
        s.resize(pad_size, 0);
        NTL::BytesFromZZ((uint8_t*) &s[0], x, s.length());
        return s;
}

inline NTL::ZZ
ZZFromString(const std::string &s)
{
        return NTL::ZZFromBytes((const uint8_t *) s.data(), s.length());
}

namespace std
{

template<>
struct hash<NTL::ZZ>
{
        std::size_t operator()(const NTL::ZZ &v) const
        {
                return std::hash<std::string>()(StringFromZZ(v));
        }
};

};

inline
size_t zzvec_intersection_count(const NTL::ZZVec &v1, const NTL::ZZVec &v2){
        assert(v1.BaseSize() == v2.BaseSize());

        std::unordered_set<NTL::ZZ> s(&v1[0], &v1[v1.length()]);
                            
        size_t res = std::count_if(&v2[0], &v2[v2.length()], 
                [&](const NTL::ZZ &k) {return s.find(k) != s.end();});
                
        return res;
}

inline std::string compact(const NTL::ZZ &v, size_t l){
    
        std::stringstream s;
        s << v;

        const std::string tmp = s.str();

        if( 2*l >= tmp.size() )
                return tmp;

        const std::string &s1 = tmp.substr(0,l);
        const std::string &s2 = tmp.substr(tmp.size()-l,l);
        return s1 + "..." + s2;
}

