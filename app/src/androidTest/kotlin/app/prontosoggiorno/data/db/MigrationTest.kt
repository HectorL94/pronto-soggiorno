package app.prontosoggiorno.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actualizar la app no debe borrar las pratiche de quien ya la usa. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun de_1_a_2_conserva_los_datos_y_empieza_sin_pausa() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO pratiche (id, number_encrypted, alias_encrypted, type, created_at, paused, monitoring_completed) " +
                    "VALUES (1, x'0102', NULL, 'PRATICA', 1000, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT id, number_encrypted, not_found_paused, resumed_at FROM pratiche").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
                assertEquals(listOf<Byte>(1, 2), c.getBlob(1).toList())
                assertEquals(0, c.getInt(2))
                assertTrue(c.isNull(3))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
