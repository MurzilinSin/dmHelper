package io.github.murzilinsin.dmhelper.domain

data class Participant(
    val id: Long,
    val name: String,
    val hp: Int,
    val initiative: Int,
)

data class Battle(
    val participants: List<Participant> = emptyList(),
    val round: Int = 0,
    val activeParticipantId: Long? = null,
)

sealed interface BattleAction {
    data object Create : BattleAction
    data class Add(val name: String, val hp: Int, val initiative: Int) : BattleAction
    data class Move(val participantId: Long, val toIndex: Int) : BattleAction
    data class SetHp(val participantId: Long, val hp: Int) : BattleAction
    data object Start : BattleAction
    data object NextTurn : BattleAction
    data object Finish : BattleAction
}
