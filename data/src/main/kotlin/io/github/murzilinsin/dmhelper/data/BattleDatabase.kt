package io.github.murzilinsin.dmhelper.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Entity(tableName = "battle")
internal data class BattleEntity(
    @PrimaryKey val id: Int = ACTIVE_BATTLE_ID,
    val round: Int,
    val activeParticipantId: Long?,
)

internal const val ACTIVE_BATTLE_ID = 1

@Entity(
    tableName = "participant",
    primaryKeys = ["battleId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = BattleEntity::class,
            parentColumns = ["id"],
            childColumns = ["battleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ParticipantEntity(
    val battleId: Int = ACTIVE_BATTLE_ID,
    val id: Long,
    val name: String,
    val hp: Int,
    val initiative: Int,
    val position: Int,
)

internal data class BattleWithParticipants(
    @Embedded val battle: BattleEntity,
    @Relation(parentColumn = "id", entityColumn = "battleId")
    val participants: List<ParticipantEntity>,
)

@Dao
internal interface BattleDao {
    @Transaction
    @Query("SELECT * FROM battle WHERE id = :id")
    suspend fun read(id: Int): BattleWithParticipants?

    @Upsert
    suspend fun saveBattle(battle: BattleEntity)

    @Upsert
    suspend fun saveParticipants(participants: List<ParticipantEntity>)

    @Query("DELETE FROM battle WHERE id = :id")
    suspend fun delete(id: Int)
}

@Database(
    entities = [BattleEntity::class, ParticipantEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class BattleDatabase : RoomDatabase() {
    abstract fun battleDao(): BattleDao
}
