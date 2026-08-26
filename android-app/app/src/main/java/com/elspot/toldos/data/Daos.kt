package com.elspot.toldos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre COLLATE NOCASE")
    fun observeAll(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ClienteEntity?

    @Query("SELECT * FROM clientes ORDER BY nombre COLLATE NOCASE")
    suspend fun allOnce(): List<ClienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ClienteEntity)

    @Update
    suspend fun update(entity: ClienteEntity)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()
}

@Dao
interface ToldoDao {
    @Query("SELECT * FROM toldos ORDER BY nombre COLLATE NOCASE")
    fun observeAll(): Flow<List<ToldoEntity>>

    @Query("SELECT * FROM toldos WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ToldoEntity?

    @Query("SELECT * FROM toldos ORDER BY nombre COLLATE NOCASE")
    suspend fun allOnce(): List<ToldoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ToldoEntity)

    @Update
    suspend fun update(entity: ToldoEntity)

    @Query("UPDATE toldos SET estado = :estado WHERE id = :id")
    suspend fun updateStatus(id: String, estado: String)

    @Query("DELETE FROM toldos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM toldos")
    suspend fun deleteAll()
}

@Dao
interface AlquilerDao {
    @Query("SELECT * FROM alquileres ORDER BY creadoEn DESC")
    fun observeAll(): Flow<List<AlquilerEntity>>

    @Query("SELECT * FROM alquileres WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AlquilerEntity?

    @Query("SELECT * FROM alquileres ORDER BY creadoEn DESC")
    suspend fun allOnce(): List<AlquilerEntity>

    @Query("SELECT * FROM alquiler_items WHERE alquilerId = :alquilerId ORDER BY linea")
    suspend fun itemsFor(alquilerId: String): List<AlquilerItemEntity>

    @Query("SELECT * FROM alquiler_items WHERE alquilerId = :alquilerId ORDER BY linea")
    fun observeItemsFor(alquilerId: String): Flow<List<AlquilerItemEntity>>

    @Query("SELECT * FROM alquiler_items ORDER BY alquilerId, linea")
    suspend fun allItemsOnce(): List<AlquilerItemEntity>

    @Query("SELECT * FROM alquiler_items ORDER BY alquilerId, linea")
    fun observeAllItems(): Flow<List<AlquilerItemEntity>>

    @Query("SELECT i.* FROM alquiler_items i INNER JOIN alquileres a ON a.id = i.alquilerId WHERE a.estado IN ('ACTIVE', 'DELIVERED')")
    suspend fun activeItems(): List<AlquilerItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlquilerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<AlquilerItemEntity>)

    @Update
    suspend fun update(entity: AlquilerEntity)

    @Query("DELETE FROM alquiler_items WHERE alquilerId = :alquilerId")
    suspend fun deleteItems(alquilerId: String)

    @Query("DELETE FROM alquileres WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM alquiler_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM alquileres")
    suspend fun deleteAll()
}

@Dao
interface ReciboDao {
    @Query("SELECT * FROM recibos ORDER BY emitidoEn DESC")
    fun observeAll(): Flow<List<ReciboEntity>>

    @Query("SELECT * FROM recibos ORDER BY emitidoEn DESC")
    suspend fun allOnce(): List<ReciboEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReciboEntity)

    @Query("DELETE FROM recibos")
    suspend fun deleteAll()
}

@Dao
interface BitacoraDao {
    @Query("SELECT * FROM bitacora ORDER BY fecha DESC LIMIT 300")
    fun observeAll(): Flow<List<BitacoraEntity>>

    @Query("SELECT * FROM bitacora ORDER BY fecha DESC LIMIT 300")
    suspend fun allOnce(): List<BitacoraEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BitacoraEntity)

    @Query("DELETE FROM bitacora")
    suspend fun deleteAll()
}
