
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>

#include <NTL/ZZ.h>

#include <utils.h>
#include <ssl_utils.h>
#include <zz.h>
#include <group_ecc_ssl.h>
#include <psica_base.h>
#include <ecc_defaults.h>

using namespace psicrypto;
using namespace std;
using namespace NTL;

typedef EccSslNist224 _group_class;
typedef PSICA_Client<_group_class> _Client;
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
    if(argc != 3){
        cout << "Usage: " << *argv << " <client-key-file> <server-key-file>" << endl;
        return -1;
    }

    ssl_init();

    string client_key_path(argv[1]); 
    string server_key_path(argv[2]); 

    const vector<string> client_set = { "a", "hello", "world", "ciao" };
    const vector<string> server_set = { "a", "hello", "y" , "ciao"};

    _group_class group1 = _group_class();
    _group_class group2 = _group_class();
    
    const auto &client_key = open_or_generate_client_key(group1, client_key_path);
    const auto &server_key = open_or_generate_server_key(group2, server_key_path);
  
    _Client client(group1, client_key);
    _Server server(group2);

    server.update(server_set);

    const auto &server_ctx = server.build_context(server_key);

    // First phase of the client: compute encryption operations on a set
    const auto &req = client.proc_elements_mp(client_set);

    // Call server routine to process client output X and HC_RCP

    const auto &resp = server.proc_elements_mp(req, server_key);
 
    const auto &tcv = client.proc_answers_mp(resp);

    // using ntl just to easily print integers values
    
    vector<NTL::ZZ> buff;
    for(auto it = resp.tsw.begin() ; it != resp.tsw.end() ; ++it)
        buff.push_back(ZZFromString(*it));
        
    cout << "tsw:" << endl << "\t";
    print_vector_compact(buff, 8, "\n\t");
    
    buff.clear();
    for(auto it = tcv.begin() ; it != tcv.end() ; ++it)
        buff.push_back(ZZFromString(*it));

    cout << "tcv:" << endl << "\t";
    cout << hex;
    print_vector_compact(buff, 8, "\n\t");
    cout << dec;
 
    cout << "Intersection cardinality: " << 
        intersection_count(resp.tsw, tcv) << endl;
    
    ssl_close();
    
    return 0;
}
