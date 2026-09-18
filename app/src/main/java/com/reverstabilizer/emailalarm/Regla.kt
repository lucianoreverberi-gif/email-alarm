package com.reverstabilizer.emailalarm

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Una regla de alarma.
 *
 * [remitente] y [palabraClave] son opcionales por separado, pero al menos uno
 * tiene que estar cargado. Si estan los dos, alcanza con que se cumpla uno (O).
 * Suena de mas antes que perderse un correo.
 */
@Entity(tableName = "reglas")
data class Regla(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** Se busca en el nombre visible Y en la direccion del remitente. */
    val remitente: String = "",
    /** Se busca en el asunto y en el resumen del cuerpo. */
    val palabraClave: String = "",
    val activa: Boolean = true,
    /** Uri del tono elegido. null = el tono de alarma del sistema. */
    val sonido: String? = null
)

/** Que paso con un correo que la app vio. */
// AVISO_CUENTA ya no se guarda; queda para leer historiales viejos.
enum class Resultado { SONO, SIN_COINCIDENCIA, SIN_SUSCRIPCION, AVISO_CUENTA }

/**
 * Un correo que la app vio, con lo que decidio. Se guardan los ultimos
 * [Deteccion.MAXIMO], solo en el telefono.
 *
 * Sirve para dos cosas: que la persona vea que la app esta viva, y poder
 * diagnosticar un "no sono": si el correo llego, si la app lo vio, si
 * coincidia con alguna regla.
 */
@Entity(tableName = "detecciones")
data class Deteccion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hora: Long,
    val app: String,
    val remitente: String,
    val asunto: String,
    /** Nombre de un [Resultado]. */
    val resultado: String,
    /** Nombre de la regla que coincidio, si coincidio alguna. */
    val regla: String? = null
) {
    companion object {
        const val MAXIMO = 20
    }
}

@Dao
interface ReglaDao {

    @Query("SELECT * FROM reglas ORDER BY id DESC")
    fun observarTodas(): Flow<List<Regla>>

    @Query("SELECT * FROM reglas WHERE activa = 1")
    suspend fun activas(): List<Regla>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(regla: Regla)

    @Update
    suspend fun actualizar(regla: Regla)

    @Delete
    suspend fun borrar(regla: Regla)
}

@Dao
interface DeteccionDao {

    @Query("SELECT * FROM detecciones ORDER BY hora DESC LIMIT ${Deteccion.MAXIMO}")
    fun observarUltimas(): Flow<List<Deteccion>>

    @Insert
    suspend fun guardar(deteccion: Deteccion)

    /** Deja solo las mas nuevas: el historial no crece para siempre. */
    @Query(
        "DELETE FROM detecciones WHERE id NOT IN " +
            "(SELECT id FROM detecciones ORDER BY hora DESC LIMIT ${Deteccion.MAXIMO})"
    )
    suspend fun recortar()

    @Query("DELETE FROM detecciones")
    suspend fun borrarTodo()
}

/**
 * De la version 1 a la 2: sonido por regla e historial de detecciones.
 *
 * Se agrega sobre lo que hay en vez de borrar y recrear la base: las reglas
 * que la persona ya cargo no se pueden perder por una actualizacion.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reglas ADD COLUMN sonido TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS detecciones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "hora INTEGER NOT NULL, " +
                "app TEXT NOT NULL, " +
                "remitente TEXT NOT NULL, " +
                "asunto TEXT NOT NULL, " +
                "resultado TEXT NOT NULL, " +
                "regla TEXT)"
        )
    }
}

@Database(entities = [Regla::class, Deteccion::class], version = 2, exportSchema = false)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun reglaDao(): ReglaDao
    abstract fun deteccionDao(): DeteccionDao

    companion object {
        @Volatile
        private var instancia: BaseDeDatos? = null

        fun obtener(context: Context): BaseDeDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDeDatos::class.java,
                    "mailalarm.db"
                ).addMigrations(MIGRACION_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
