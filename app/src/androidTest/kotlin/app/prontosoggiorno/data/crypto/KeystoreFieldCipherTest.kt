package app.prontosoggiorno.data.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.prontosoggiorno.data.crypto.FieldCipher.Purpose
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreFieldCipherTest {

    private val cipher = KeystoreFieldCipher(keyAlias = "test_cipher_key")

    @After
    fun cleanUp() = cipher.deleteKey()

    @Test
    fun cifra_y_descifra() {
        val blob = cipher.encrypt("00ZZ000000", Purpose.PRATICA_NUMBER)
        assertEquals("00ZZ000000", cipher.decrypt(blob, Purpose.PRATICA_NUMBER))
    }

    @Test
    fun el_texto_cifrado_no_contiene_el_numero_y_cambia_cada_vez() {
        val a = cipher.encrypt("00ZZ000000", Purpose.PRATICA_NUMBER)
        val b = cipher.encrypt("00ZZ000000", Purpose.PRATICA_NUMBER)
        assertFalse(String(a, Charsets.ISO_8859_1).contains("00ZZ000000"))
        assertFalse("IV aleatorio: dos cifrados iguales no deben coincidir", a.contentEquals(b))
    }

    @Test
    fun un_numero_no_se_puede_leer_como_alias() {
        val blob = cipher.encrypt("00ZZ000000", Purpose.PRATICA_NUMBER)
        assertNull(cipher.decrypt(blob, Purpose.ALIAS))
    }

    @Test
    fun datos_manipulados_o_truncados_devuelven_null() {
        val blob = cipher.encrypt("Mamma", Purpose.ALIAS)
        val tampered = blob.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(cipher.decrypt(tampered, Purpose.ALIAS))
        assertNull(cipher.decrypt(blob.copyOf(5), Purpose.ALIAS))
        assertNull(cipher.decrypt(ByteArray(0), Purpose.ALIAS))
    }

    @Test
    fun sin_la_clave_los_datos_quedan_ilegibles() {
        val blob = cipher.encrypt("00ZZ000000", Purpose.PRATICA_NUMBER)
        cipher.deleteKey() // simula reinstalación / datos borrados
        assertNull(cipher.decrypt(blob, Purpose.PRATICA_NUMBER))
    }
}
