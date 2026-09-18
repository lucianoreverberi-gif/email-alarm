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
    val activa: Boolean = true
)

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

@Database(entities = [Regla::class], version = 1, exportSchema = false)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun reglaDao(): ReglaDao

    companion object {
        @Volatile
        private var instancia: BaseDeDatos? = null

        fun obtener(context: Context): BaseDeDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDeDatos::class.java,
                    "mailalarm.db"
                ).build().also { instancia = it }
            }
    }
}
