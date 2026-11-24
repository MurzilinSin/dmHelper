package io.github.murzilinsin.dmhelper.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.murzilinsin.dmhelper.domain.BattleAction
import io.github.murzilinsin.dmhelper.domain.BattleRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class BattleViewModel @Inject constructor(
    private val repository: BattleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BattleUiState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: BattleIntent) {
        val current = state.value
        if (current.loading || current.saving) return
        if (intent != BattleIntent.Retry && current.error == BattleError.Load) return

        when (intent) {
            BattleIntent.Retry -> load()
            BattleIntent.Create -> save(BattleAction.Create)
            BattleIntent.OpenAdd -> showDialog(BattleDialog.Add())
            is BattleIntent.OpenEditHp -> {
                val participant = current.battle?.participants?.find { it.id == intent.participantId }
                if (participant != null) {
                    showDialog(BattleDialog.EditHp(participant.id, participant.name, participant.hp.toString()))
                }
            }
            is BattleIntent.ChangeName -> editAdd { it.copy(name = intent.value, error = null) }
            is BattleIntent.ChangeInitiative -> editAdd { it.copy(initiative = intent.value, error = null) }
            is BattleIntent.ChangeHp -> when (val dialog = current.dialog) {
                is BattleDialog.Add -> editAdd { it.copy(hp = intent.value, error = null) }
                is BattleDialog.EditHp -> showDialog(dialog.copy(hp = intent.value, invalidHp = false))
                else -> Unit
            }
            BattleIntent.SubmitDialog -> submitDialog(current.dialog)
            BattleIntent.DismissDialog -> showDialog(null)
            is BattleIntent.Move -> save(BattleAction.Move(intent.participantId, intent.toIndex))
            BattleIntent.Start -> save(BattleAction.Start)
            BattleIntent.NextTurn -> save(BattleAction.NextTurn)
            BattleIntent.ConfirmFinish -> showDialog(BattleDialog.Finish)
        }
    }

    private fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val battle = repository.load()
                mutableState.value = BattleUiState(battle = battle, loading = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = BattleError.Load) }
            }
        }
    }

    private fun save(action: BattleAction) {
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val battle = repository.apply(action)
                mutableState.update { it.copy(battle = battle, saving = false, dialog = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, error = BattleError.Save) }
            }
        }
    }

    private fun showDialog(dialog: BattleDialog?) {
        mutableState.update { it.copy(dialog = dialog, error = null) }
    }

    private fun editAdd(transform: (BattleDialog.Add) -> BattleDialog.Add) {
        val dialog = state.value.dialog as? BattleDialog.Add ?: return
        showDialog(transform(dialog))
    }

    private fun submitDialog(dialog: BattleDialog?) {
        when (dialog) {
            is BattleDialog.Add -> {
                val hp = dialog.hp.trim().toIntOrNull()?.takeIf { it >= 0 }
                val initiative = dialog.initiative.trim().toIntOrNull()
                when {
                    dialog.name.isBlank() -> showDialog(dialog.copy(error = InputError.Name))
                    hp == null -> showDialog(dialog.copy(error = InputError.Hp))
                    initiative == null -> showDialog(dialog.copy(error = InputError.Initiative))
                    else -> save(BattleAction.Add(dialog.name, hp, initiative))
                }
            }
            is BattleDialog.EditHp -> {
                val hp = dialog.hp.trim().toIntOrNull()?.takeIf { it >= 0 }
                if (hp == null) {
                    showDialog(dialog.copy(invalidHp = true))
                } else {
                    save(BattleAction.SetHp(dialog.participantId, hp))
                }
            }
            BattleDialog.Finish -> save(BattleAction.Finish)
            null -> Unit
        }
    }
}
