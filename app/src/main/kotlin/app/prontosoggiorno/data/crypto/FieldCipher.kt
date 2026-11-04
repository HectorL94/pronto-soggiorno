package app.prontosoggiorno.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cifra campos sensibles (número de pratica, alias) antes de guardarlos en la base de datos. */
interface FieldCipher {
    fun encrypt(plain: String, purpose: Purpose): ByteArray

    /** Devuelve null si no se puede descifrar (clave perdida, datos dañados o manipulados). */
    fun decrypt(blob: ByteArray, purpose: Purpose): String?

    /**
     * El propósito se vincula al cifrado (AAD de AES-GCM): un número cifrado no puede
     * hacerse pasar por un alias ni al revés.
     */
    enum class Purpose(val aad: ByteArray) {
        PRATICA_NUMBER("pratica-number/v1".toByteArray()),
        ALIAS("alias/v1".toByteArray()),
    }
}

/**
 * Implementación con una clave AES-256 del Android Keystore.
 * - La clave se genera dentro del Keystore y nunca sale de él (no es exportable).
 * - No exige desbloqueo del teléfono: el trabajo en segundo plano debe poder leer el número.
 * - Si se borran los datos de la app o se reinstala, la clave desaparece y lo cifrado queda
 *   ilegible (decrypt -> null). Es intencionado: no hay copias de seguridad.
 *
 * Formato del blob: [versión=1][longitud IV][IV][texto cifrado + etiqueta GCM].
 */
class KeystoreFieldCipher(private val keyAlias: String = DEFAULT_ALIAS) : FieldCipher {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    @Synchronized
    private fun key(): SecretKey =
        (keyStore.getKey(keyAlias, null) as SecretKey?) ?: generateKey()

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    override fun encrypt(plain: String, purpose: FieldCipher.Purpose): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key()) // el Keystore genera un IV aleatorio
        cipher.updateAAD(purpose.aad)
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return ByteBuffer.allocate(2 + iv.size + ciphertext.size)
            .put(FORMAT_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
    }

    override fun decrypt(blob: ByteArray, purpose: FieldCipher.Purpose): String? = try {
        val buffer = ByteBuffer.wrap(blob)
        if (buffer.get() != FORMAT_VERSION) {
            null
        } else {
            val iv = ByteArray(buffer.get().toInt()).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(purpose.aad)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: RuntimeException) {
        null // blob truncado o con longitudes inválidas
    }

    /** Solo para tests: elimina la clave. */
    internal fun deleteKey() = keyStore.deleteEntry(keyAlias)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_ALIAS = "pronto_soggiorno_fields_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val FORMAT_VERSION: Byte = 1
    }
}
