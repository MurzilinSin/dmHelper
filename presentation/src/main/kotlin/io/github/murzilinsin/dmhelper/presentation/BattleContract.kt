package io.github.murzilinsin.dmhelper.presentation

import io.github.murzilinsin.dmhelper.domain.Battle

internal data class BattleUiState(
    val battle: Battle? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: BattleError? = null,
    val dialog: BattleDialog? = null,
)

internal enum class BattleError { Load, Save }
internal enum class InputError { Name, Hp, Initiative }

internal sealed interface BattleDialog {
    data class Add(
        val name: String = "",
        val hp: String = "",
        val initiative: String = "",
        val error: InputError? = null,
    ) : BattleDialog

    data class EditHp(
        val participantId: Long,
        val name: String,
        val hp: String,
        val invalidHp: Boolean = false,
    ) : BattleDialog

    data object Finish : BattleDialog
}

internal sealed interface BattleIntent {
    data object Retry : BattleIntent
    data object Create : BattleIntent
    data object OpenAdd : BattleIntent
    data class OpenEditHp(val participantId: Long) : BattleIntent
    data class ChangeName(val value: String) : BattleIntent
    data class ChangeHp(val value: String) : BattleIntent
    data class ChangeInitiative(val value: String) : BattleIntent
    data object SubmitDialog : BattleIntent
    data object DismissDialog : BattleIntent
    data class Move(val participantId: Long, val toIndex: Int) : BattleIntent
    data object Start : BattleIntent
    data object NextTurn : BattleIntent
    data object ConfirmFinish : BattleIntent
}
