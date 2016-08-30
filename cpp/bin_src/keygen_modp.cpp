
#include <string>
#include <fstream>
#include <iostream>
#include <sstream>

#include <psica_base.h>
#include <group_dp_zz.h>

using namespace std;
using namespace psicrypto;

typedef PSICA_Key<DP_ZZ::exp_type> KEY;

int main(int argc, char *argv[]){
        if(argc != 3){
                cout << "Usage: " << *argv << " <shared-input-file>"
                        " <key-output-file>" << endl;
                return -1;
        }

        string shared_path(argv[1]);
        string key_path(argv[2]); 

        ifstream shared_f;
        shared_f.open(shared_path);
        const DP_ZZ group(DP_ZZ::Pars::load(shared_f));
        shared_f.close();

        const auto &k = PSICA_Server<DP_ZZ>::keygen(group);

        if(key_path == "-")
        {
                KEY::save(cout, k);
        }
        else
        {    
                ofstream key_f;
                key_f.open(key_path);
                KEY::save(key_f, k);
                key_f.close();
        }
        return 0;
}

