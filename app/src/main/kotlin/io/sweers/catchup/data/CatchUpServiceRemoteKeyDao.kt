package io.sweers.catchup.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Dao
interface CatchUpServiceRemoteKeyDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(keys: CatchUpServiceRemoteKey)

  @Query("SELECT * FROM remote_keys WHERE serviceId = :serviceId")
  suspend fun remoteKeyByService(serviceId: String): CatchUpServiceRemoteKey

  @Query("DELETE FROM remote_keys WHERE serviceId = :serviceId")
  suspend fun deleteByService(serviceId: String)
}

@Entity(tableName = "remote_keys")
data class CatchUpServiceRemoteKey(
  @PrimaryKey
  val serviceId: String,
  val nextPageKey: String?
)