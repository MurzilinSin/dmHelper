package io.github.murzilinsin.dmhelper.domain

object BattleRules {
    fun apply(battle: Battle?, action: BattleAction): Battle? {
        return when (action) {
            BattleAction.Create -> battle ?: Battle()
            BattleAction.Finish -> null
            else -> battle?.let { change(it, action) }
        }
    }

    private fun change(battle: Battle, action: BattleAction): Battle {
        return when (action) {
            is BattleAction.Add -> battle.add(action)
            is BattleAction.Move -> battle.move(action)
            is BattleAction.SetHp -> {
                require(action.hp >= 0)
                battle.copy(participants = battle.participants.map { participant ->
                    if (participant.id == action.participantId) {
                        participant.copy(hp = action.hp)
                    } else {
                        participant
                    }
                })
            }
            BattleAction.Start -> {
                if (battle.round == 0 && battle.participants.isNotEmpty()) {
                    battle.copy(round = 1, activeParticipantId = battle.participants.first().id)
                } else {
                    battle
                }
            }
            BattleAction.NextTurn -> battle.nextTurn()
            BattleAction.Create, BattleAction.Finish -> battle
        }
    }

    private fun Battle.add(action: BattleAction.Add): Battle {
        val name = action.name.trim()
        require(name.isNotEmpty() && action.hp >= 0)
        val id = (participants.maxOfOrNull { it.id } ?: 0) + 1
        val newParticipant = Participant(id, name, action.hp, action.initiative)
        val position = participants.indexOfFirst { it.initiative < action.initiative }
        val updated = participants.toMutableList()
        updated.add(if (position < 0) participants.size else position, newParticipant)
        return copy(participants = updated)
    }

    private fun Battle.move(action: BattleAction.Move): Battle {
        val fromIndex = participants.indexOfFirst { it.id == action.participantId }
        if (fromIndex < 0 || action.toIndex !in participants.indices || fromIndex == action.toIndex) {
            return this
        }
        val updated = participants.toMutableList()
        updated.add(action.toIndex, updated.removeAt(fromIndex))
        return copy(participants = updated)
    }

    private fun Battle.nextTurn(): Battle {
        val current = participants.indexOfFirst { it.id == activeParticipantId }
        if (current < 0) return this

        val next = current + 1
        return if (next == participants.size) {
            copy(round = round + 1, activeParticipantId = participants.first().id)
        } else {
            copy(activeParticipantId = participants[next].id)
        }
    }
}
