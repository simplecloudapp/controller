package app.simplecloud.controller.shared.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.*

class JwtHandler(private val secret: String, private val issuer: String) {

    /**
     * Generates a jwt token
     * @param subject the subject to sign
     * @param expiresIn time span in seconds or null if it should not expire
     * @return the JWT token
     */
    fun generateJwt(subject: String, expiresIn: Int? = null, scope: String = ""): SignedJWT {
        val claimsSet = JWTClaimsSet.Builder()
            .subject(subject)
            .claim("scope", scope)
            .issuer(issuer)
        if (expiresIn != null)
            claimsSet.expirationTime(Date(System.currentTimeMillis() + expiresIn * 1000L))
        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet.build())
    }

    /**
     * Generates a signed jwt token
     * @param subject the subject to sign
     * @param expiresIn time span in seconds or null if it should not expire
     * @return the JWT token as a signed string
     */
    fun generateJwtSigned(subject: String, expiresIn: Int? = null, scope: String = ""): String {
        val signer = MACSigner(secret.toByteArray())
        val signedJWT = generateJwt(subject, expiresIn, scope)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    /**
     * @return Whether the provided token was signed by this handler or not
     */
    fun verifyJwt(token: String): Boolean {
        val signedJWT = SignedJWT.parse(token)
        val verifier = MACVerifier(secret.toByteArray())
        return signedJWT.verify(verifier)
    }

    fun decodeJwt(token: String): SignedJWT {
        return SignedJWT.parse(token)
    }
}