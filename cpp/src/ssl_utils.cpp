#include <ssl_utils.h>

static pthread_mutex_t *lock_cs=NULL;
static long *lock_count=NULL;

void pthreads_locking_callback(int mode, int type, const char *file, int line)
{
        if (mode & CRYPTO_LOCK) 
        {
                pthread_mutex_lock(&(lock_cs[type]));
                lock_count[type]++;
        } 
        else
        {
                pthread_mutex_unlock(&(lock_cs[type]));
        }
}

void thread_setup(void)
{
        lock_cs = (pthread_mutex_t*) OPENSSL_malloc(
                CRYPTO_num_locks() * (int)sizeof(pthread_mutex_t) );
        lock_count = (long*) OPENSSL_malloc(
                CRYPTO_num_locks() * (int)sizeof(long) );

        for (int i=0 ; i < CRYPTO_num_locks() ; i++ )
        {
                lock_count[i]=0;
                pthread_mutex_init(&(lock_cs[i]), NULL);
        }

        CRYPTO_set_id_callback( 
                (unsigned long (*)(void)) pthreads_thread_id );
        CRYPTO_set_locking_callback( 
                 pthreads_locking_callback);
}

void thread_cleanup(void) 
{
        int i;
        CRYPTO_set_locking_callback(NULL);
        for (i=0 ; i < CRYPTO_num_locks() ; i++ )
        {
                pthread_mutex_destroy(&(lock_cs[i]));
        }
        OPENSSL_free(lock_cs);
        OPENSSL_free(lock_count);
}

int ssl_init(void)
{
        SSL_load_error_strings();
        OpenSSL_add_ssl_algorithms();
        #ifdef OPENSSL_THREADS
                thread_setup();
        #endif 
        return 1;
}

int ssl_close(void)
{
        #ifdef OPENSSL_THREADS
                thread_cleanup();
        #endif
        ERR_free_strings();
        EVP_cleanup();
        return 1;
}


psicrypto::WBN::WBN()
{
        p = BN_new();
        assert(p);
}

psicrypto::WBN::WBN(const psicrypto::WBN &v) : WBN()
{
        BN_copy(this->p, v.p);
}

psicrypto::WBN::~WBN()
{
        BN_free(p);
}

psicrypto::WBN& psicrypto::WBN::operator=(const psicrypto::WBN &rhs)
{
        BN_copy(this->p, rhs.p);
        return *this;
}

bool psicrypto::WBN::operator==(const psicrypto::WBN &rhs) const
{
        return 0 == BN_cmp(this->p, rhs.p);
}

std::ostream& psicrypto::operator<<(std::ostream &os, const psicrypto::WBN &obj)
{
        // https://www.openssl.org/docs/crypto/BN_bn2bin.html
        // this seems quite inefficient, but I see no alternatives
        char *buff = BN_bn2dec(obj.p);
        std::string s(buff); // string copy constructor for NULL-terminated strings
        OPENSSL_free(buff);
        os << s;
        return os;
}

std::istream& psicrypto::operator>>(std::istream &is, psicrypto::WBN &obj)
{
        std::string s;
        is >> s;
        BN_dec2bn(&obj.p, s.c_str());
        return is;
}

psicrypto::WECP::WECP(const EC_GROUP *g) : g(g)
{
        assert(NULL != g);
        p = EC_POINT_new(g);
}

psicrypto::WECP::WECP(const psicrypto::WECP &rhs) : WECP(rhs.g)
{
        assert(this->p);
        EC_POINT_copy(this->p, rhs.p);
}

psicrypto::WECP::~WECP()
{
        EC_POINT_free(p);
}

psicrypto::WECP& psicrypto::WECP::operator=(const psicrypto::WECP &rhs)
{
        EC_POINT_copy(this->p, rhs.p);
        return *this;
}

bool psicrypto::WECP::operator==(const psicrypto::WECP &rhs) const
{
        thread_local WBNCTX ctx;
        BN_CTX_start(ctx.p);
        // assuming that eventually the comparison can accept points of different curves
        // TODO: this is a const method but we pass non-const ctx?
        bool r = 0 == EC_POINT_cmp(g, this->p, rhs.p, ctx.p);
        BN_CTX_end(ctx.p);
        return r;
}

std::string psicrypto::WECP::to_str(void) const
{
        thread_local WBNCTX ctx;
        thread_local WBN x, y;

        BN_CTX_start(ctx.p);

        EC_POINT_get_affine_coordinates_GFp(this->g, this->p, x.p, y.p, ctx.p);

        BN_CTX_end(ctx.p);

        std::stringstream ss;
        ss << x << ":" << y;
        return ss.str();
}

void psicrypto::WECP::from_str(const std::string &s)
{
        thread_local WBNCTX ctx;
        thread_local WBN x, y;
        size_t pos = s.find(":");

        std::stringstream ss;
        ss << s.substr(0, pos);
        ss >> x;
        ss << s.substr(pos+1, s.size());
        ss >> y;

        BN_CTX_start(ctx.p);
        EC_POINT_set_affine_coordinates_GFp(this->g, this->p, x.p, y.p, ctx.p);
        BN_CTX_end(ctx.p);
}

std::ostream& psicrypto::operator<<(std::ostream &os, const psicrypto::WECP &obj)
{
        os << obj.to_str();        
        return os;
}

std::istream& psicrypto::operator>>(std::istream &is, psicrypto::WECP &obj)
{
        std::stringstream ss;
        ss << is;
        obj.from_str(ss.str());
        return is;
}

psicrypto::WBNCTX::WBNCTX()
{
        p = BN_CTX_new();
}

psicrypto::WBNCTX::~WBNCTX()
{
        BN_CTX_free(this->p);
}

