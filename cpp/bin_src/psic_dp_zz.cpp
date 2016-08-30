#include <iostream>
#include <fstream>
#include <string>
#include <sstream>

#include <NTL/ZZ.h>

#include <utils.h>
#include <zz.h>
#include <psica_base.h>
#include <psica_modp.h>
#include <group_dp_zz.h>
#include <standards.h>

using namespace std;
using namespace psicrypto;
using namespace NTL;

#ifdef TIMINGS
#include <chrono>
#include <ctime>

using namespace chrono;

#define HRC high_resolution_clock

#endif

typedef DP_ZZ _group_class;
typedef PSICA_MODP_Client<_group_class> _Client;
typedef PSICA_Server<_group_class> _Server;

inline _Client::KEY
open_or_generate_client_key(const _Client::GROUP &group, const std::string &s)
{
        _Client::KEY key;
        if(s == "-")
        {
                key = _Client::keygen(group);
        } else {
                std::ifstream f;
                f.open(s);
                _Client::KEY::load(f, key);
                f.close();
        }

        return key;
}

inline _Server::KEY
open_or_generate_server_key(const _Server::GROUP &group, const std::string &s)
{
        _Client::KEY key;
        if(s == "-")
        {
                key = _Server::keygen(group);
        } else {
                std::ifstream f;
                f.open(s);
                _Server::KEY::load(f, key);
                f.close();
        }

        return key;
}

int main(int argc, char *argv[])
{
        if(argc != 6){
                cerr << "Usage: " << *argv << " <id={22|23|24}> " 
                        "<client-data> <server-data> <client-key-file> " 
                        "<server-key-file>" << endl;
                return -1;
        }

        #ifdef TIMINGS
        HRC::time_point ti, tf;
        #endif

        string id(argv[1]);
        string client_data_path(argv[2]); 
        string server_data_path(argv[3]); 
        string client_key_path(argv[4]); 
        string server_key_path(argv[5]); 
            
        ifstream f;

        _group_class::Pars pars;
    
        if(id == "22")
        {
                nist22(pars.p, pars.q, pars.g);
        }
        else if(id == "23")
        {
                nist23(pars.p, pars.q, pars.g);
        }
        else if(id == "24")
        {  
                nist24(pars.p, pars.q, pars.g);
        }
        else
        {
                cerr << "Error: invalid security id " << id << endl;
                return -1; 
        }

        DP_ZZ gc(pars.p, pars.q, pars.g), gs(pars.p, pars.q, pars.g);

        const auto &client_key = open_or_generate_client_key(gc, client_key_path);
        const auto &server_key = open_or_generate_server_key(gs, server_key_path);

        vector<string> client_set;
        vector<string> server_set;

        string buffer;

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        #pragma omp parallel sections private(f, buffer)
        {
            #pragma omp section
            {
                f.open(client_data_path);
                while(getline(f, buffer))
                {
                        client_set.push_back(buffer);
                }
                f.close();
            }
            
            #pragma omp section
            {
                f.open(server_data_path);
                while(getline(f, buffer))
                {
                        server_set.push_back(buffer);
                }
                f.close();
            }
        
        }

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Data load time:                  " << time_diff(tf,ti) << endl;
        #endif

        cout << "Plaintext intersection:          " << 
                intersection_count(client_set, server_set) << endl;

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        // The client is initialized with shared pars and key (random rc and rcp)
        _Client client(gc, client_key);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Client class init time:          " << time_diff(tf,ti) << endl;
        #endif


        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        // First phase of the client: compute encryption operations on a set
        const auto &req = client.proc_elements_mp(client_set);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Client dataset proc time:        " << time_diff(tf,ti) << endl;
        #endif

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        // The server is initialized with shared pars and the set of elements
        _Server server(gs);
        server.update(server_set);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Server class init time:          " << time_diff(tf,ti) << endl;
        #endif

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        // Build the server context wrt to the key
        const auto &ctx = server.build_context(server_key);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Server context building time:    " << time_diff(tf,ti) << endl;
        #endif

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        const auto &resp = server.proc_elements_mp(req, server_key, ctx);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Server client request proc time: " << time_diff(tf,ti) << endl;
        #endif

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        const auto &tcv = client.proc_answers_mp(resp);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "Client server answer proc time:  " << time_diff(tf,ti) << endl;
        #endif

        #ifdef TIMINGS
        ti = HRC::now();
        #endif

        size_t c = intersection_count(resp.tsw, tcv);

        #ifdef TIMINGS
        tf = HRC::now();
        cerr << "intersection time:               " << time_diff(tf,ti) << endl;
        #endif


        cout << "Intersection cardinality:        " << c << endl;

        return 0;
}
