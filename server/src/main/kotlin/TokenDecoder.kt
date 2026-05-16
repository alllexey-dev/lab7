import util.MD2Hasher
import java.time.LocalTime

typealias UserHashed = String
typealias Token = String

class TokenDecoder {
    private val hm = BiMap<Token, UserHashed>()
    private val tokenExpirationMap = HashMap<Token, LocalTime>()

    fun updateToken(userHash: UserHashed): String {
        val token = MD2Hasher.getMD2Hash(userHash + LocalTime.now())
        hm.insertKeyByValue(userHash, token)
        tokenExpirationMap[token] = LocalTime.now().plusMinutes(15)
        return token
    }

    fun matchToken(token: Token): String {

        val user = hm.getValueByKey(token)!!
        val time = tokenExpirationMap[token]!!

        if (time < LocalTime.now()) {
            throw TokenExpiredException()
        } else{
            tokenExpirationMap[token] = LocalTime.now().plusMinutes(15)
        }

        return user
    }
}

private class BiMap<K, V> {
    private var forward = HashMap<K, V>()
    private var backward = HashMap<V, K>()


    fun getValueByKey(k: K): V? {
        return forward[k]
    }


    fun insertKeyByValue(v: V, k: K) {
        forward[k]?.let{
            backward.remove(it)
        }
        backward[v]?.let {
            forward.remove(it)
        }

        forward[k] = v
        backward[v] = k
    }
}