package io.github.murzilinsin.dmhelper.data

import androidx.room.withTransaction
import io.github.murzilinsin.dmhelper.domain.Battle
import io.github.murzilinsin.dmhelper.domain.BattleAction
import io.github.murzilinsin.dmhelper.domain.BattleRepository
import io.github.murzilinsin.dmhelper.domain.BattleRules
import io.github.murzilinsin.dmhelper.domain.Participant
import javax.inject.Inject

internal class RoomBattleRepository @Inject constructor(
    private val database: BattleDatabase,
) : BattleRepository {
    private val dao = database.battleDao()

    override suspend fun load(): Battle? = dao.read(ACTIVE_BATTLE_ID)?.toDomain()

    override suspend fun apply(action: BattleAction): Battle? =
        database.withTransaction {
            val current = dao.read(ACTIVE_BATTLE_ID)?.toDomain()
            val updated = BattleRules.apply(current, action)
            if (updated == current) return@withTransaction current

            if (updated == null) {
                dao.delete(ACTIVE_BATTLE_ID)
            } else {
                dao.saveBattle(BattleEntity(
                    round = updated.round,
                    activeParticipantId = updated.activeParticipantId,
                ))
                if (updated.participants != current?.participants && updated.participants.isNotEmpty()) {
                    dao.saveParticipants(updated.participants.mapIndexed { position, participant ->
                        ParticipantEntity(
                            id = participant.id,
                            name = participant.name,
                            hp = participant.hp,
                            initiative = participant.initiative,
                            position = position,
                        )
                    })
                }
            }
            updated
        }

    private fun BattleWithParticipants.toDomain() = Battle(
        round = battle.round,
        activeParticipantId = battle.activeParticipantId,
        participants = participants.sortedBy { it.position }.map {
            Participant(id = it.id, name = it.name, hp = it.hp, initiative = it.initiative)
        },
    )
}
