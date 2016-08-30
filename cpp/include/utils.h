#pragma once

#include <fstream>
#include <iostream>
#include <vector>
#include <chrono>
#include <string>
#include <sstream>
#include <unordered_set>
#include <algorithm>

#include <assert.h>

#include <NTL/ZZ.h>
#include <NTL/ZZVec.h>

#include <zz.h>


inline 
double time_diff(const std::chrono::high_resolution_clock::time_point &end,
                 const std::chrono::high_resolution_clock::time_point &start){
    return std::chrono::duration_cast<std::chrono::duration<double>>
        (end - start).count();
}

template <typename T>
inline 
void print_vector(const std::vector<T> &v, const std::string &separator = ",", 
                  std::ostream &of=std::cout)
{
        auto it = v.begin();
        for(; it != v.end(); ++it)
        {
                if(it != v.begin())
                        of << separator;
                of << *it ;
        }
        of << std::endl;
}


inline 
void print_vector(const NTL::ZZVec &v, const std::string &separator = ", ", 
                  std::ostream &of=std::cout)
{
        for(uint i=0; i < v.length(); ++i)
        {
                if(i != 0 && separator != "")
                        of << separator;
                of << v[i];
        }
        of << std::endl;
}

inline 
void print_vector_compact(const NTL::ZZVec &v, size_t l, 
                                 const std::string &separator = ", ", 
                                 std::ostream &of=std::cout){
        for(uint i=0; i < v.length(); ++i)
        {
                if(i != 0 && separator != "")
                        of << separator;
                of << compact(v[i],5);
        }
        of << std::endl;
}

inline void print_vector_compact(const std::vector<std::string> &v, size_t l, 
                                 const std::string &separator = ", ", 
                                 std::ostream &of=std::cout)
{
        for(uint i=0; i < v.size(); ++i)
        {
                const std::string &tmp = v[i];
                if(i != 0 && separator != "")
                        of << separator;
                of << tmp.substr(0, l) << tmp.substr(tmp.size()-l, l);
        }
        of << std::endl;
}

inline
unsigned int bit_to_zzigits(unsigned long b){
//    if(b == 0){
//        throw(1);
//    }
        return (b - 1) / NTL_ZZ_NBITS + 1;
};


inline
unsigned int bytes_to_zzigits(unsigned long b){
//    if(b == 0){
//        throw(1);
//    }
        return (b*8 - 1) / NTL_ZZ_NBITS + 1;
};


inline
unsigned int zzigits_to_bytes(unsigned long zzigt){
//    if(b == 0){
//        throw(1);
//    }
        return (NTL_ZZ_NBITS * zzigt - 1) / 8 + 1;
};


inline
const std::vector<NTL::ZZ> read_key(std::istream &f){
        std::vector<NTL::ZZ> k;
        std::string buff;
        while(std::getline(f, buff)){
                k.push_back(NTL::conv<NTL::ZZ>(buff.c_str()));
        }

        return k;
}

inline
void split(const std::string &s, char delim, std::vector<std::string> &elems)
{
        std::string r="";
        bool last = false;
        for(const char &c : s) 
        {
                if( c != delim )
                {
                        r += c;
                        if( not last)
                                last = true;
                        
                } 
                else if(last)
                {
                        elems.push_back(r);
                        r = "";   
                        last = false;
                }
        }
    
        if(last)
        {
                elems.push_back(r);
        }
}

template<typename T>
size_t intersection_count(const std::vector<T> &v1, const std::vector<T> &v2)
{
        // Eventually requires definition of std::hash<T> and std::equal_to<T> 
        // for custom types
        std::unordered_set<T> s(v1.begin(), v1.end());
    
        return std::count_if(v2.begin(), v2.end(),
                [&](const T &k) {return s.find(k) != s.end(); });
}


template<typename T>
inline void print_vector_compact(const std::vector<T> &v, size_t l,
                                 const std::string &separator = ", ", 
                                 std::ostream &of=std::cout)
{
        for(auto it=v.begin(); it!=v.end(); ++it)
        {
                if(it != v.begin() && separator != "")
                        of << separator;
                of << compact(*it,5);
        }
        of << std::endl;
}

