
#include <string>
#include <fstream>
#include <iostream>
#include <sstream>

#include <psica_base.h>
#include <ecc_defaults.h>

using namespace std;
using namespace psicrypto;

typedef PSICA_Key<EccSsl::exp_type> KEY;

int main(int argc, char *argv[]){
    if(argc != 3){
        cout << "Usage: " << *argv << " <curve-id> <key-output-file>" << endl;
        return -1;
    }

    string id(argv[1]); 
    string key_path(argv[2]); 
    
    EC_GROUP *g;
    
    if(NULL == (g = get_ec_group(id)))
    {
        cerr << "Invalid curve id " << id << endl;
        return -1;
    }    
    
    const auto &k = PSICA_Server<EccSsl>::keygen(g);

    if(key_path == "-"){
        KEY::save(cout, k);
    }else{    
        ofstream key_f;
        key_f.open(key_path);
        KEY::save(key_f, k);
        key_f.close();
    }
    return 0;
}

