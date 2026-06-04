import util.MD2Hasher
import java.time.LocalDateTime

typealias UserHashed = Pair<String, String>
typealias Token = String

class TokenDecoder {
    private val hm = BiMap<Token, String>()
    private val tokenExpirationMap = HashMap<Token, LocalDateTime>()

    fun updateToken(userHash: UserHashed): String {
        val token = MD2Hasher.getMD2Hash(userHash.second + LocalDateTime.now())
        hm.insertKeyByValue(userHash.second, token)
        tokenExpirationMap[token] = LocalDateTime.now().plusMinutes(15)
        return token
    }

    fun matchToken(token: Token): String {

        val user = hm.getValueByKey(token)!!
        val time = tokenExpirationMap[token]!!

        if (time < LocalDateTime.now()) {
            throw TokenExpiredException()
        } else {
            tokenExpirationMap[token] = LocalDateTime.now().plusMinutes(15)
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
        forward[k]?.let {
            backward.remove(it)
        }
        backward[v]?.let {
            forward.remove(it)
        }

        forward[k] = v
        backward[v] = k
    }
}