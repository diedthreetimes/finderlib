
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

using namespace psicrypto;
using namespace std;
using namespace NTL;

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
        if(argc != 4){
                cout << "Usage: " << *argv << " <shared-pars-file> "
                                              "<client-key-file> "
                                              "<server-key-file> " << endl;
                return -1;
        }

        string pars_path(argv[1]);
        string client_key_path(argv[2]); 
        string server_key_path(argv[3]); 

        const vector<string> client_set = { "a", "hello", "world", "ciao" };
        const vector<string> server_set = { "a", "hello", "bye" , "ciao"};

        ifstream f;
        f.open(pars_path);
        const auto &pars = _group_class::Pars::load(f);
        f.close();

        _group_class group(pars);

        const auto &client_key = open_or_generate_client_key(group, client_key_path);
        const auto &server_key = open_or_generate_server_key(group, server_key_path);

        _Client client(group, client_key);
        _Server server(group);

        server.update(server_set);

        // Build the server context wrt to the loaded key
        // (if we do not pass the key it generates a new random one internally)
        const auto &server_ctx = server.build_context(server_key);

        print_vector_compact(server_ctx.bhw, 5);

        // First phase of the client: compute encryption operations on a set
        const auto &req = client.proc_elements_mp(client_set);

        print_vector_compact(req.av, 5);

        const auto &resp = server.proc_elements_mp(req, server_key);

        print_vector_compact(resp.apv, 5);

        const auto &tcv = client.proc_answers_mp(resp);

        cout << "Intersection cardinality: " << intersection_count(resp.tsw, tcv) << endl;

        return 0;
}
