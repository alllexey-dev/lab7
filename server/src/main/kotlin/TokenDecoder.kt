import util.MD2Hasher
import java.time.LocalTime

typealias UserHashed = String
typealias Token = Pair<String, LocalTime>

class TokenDecoder {
    val hm = HashMap<UserHashed, Token>()

    fun updateToken(userHash: UserHashed): String{
        val token = Token(MD2Hasher.getMD2Hash(userHash + LocalTime.now()), LocalTime.now().plusMinutes(15))
        hm[userHash] = token
        return token.first
    }

    fun matchToken(userHash: UserHashed): String{
        if (hm.contains(userHash)){
            if (hm[userHash]!!.second < LocalTime.now()){
                throw TokenExpiredException()
            }
            println("$userHash expires at ${hm[userHash]!!.second}")
            val token = Token(hm[userHash]!!.first, LocalTime.now().plusMinutes(15))
            hm[userHash] = token
            return token.first
        }
        else{
            val token = Token(MD2Hasher.getMD2Hash(userHash + LocalTime.now()), LocalTime.now().plusMinutes(15))
            hm[userHash] = token
            return token.first
        }
    }
}