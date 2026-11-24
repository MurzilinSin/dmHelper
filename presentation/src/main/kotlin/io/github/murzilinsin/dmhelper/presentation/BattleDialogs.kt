package io.github.murzilinsin.dmhelper.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun BattleDialogs(state: BattleUiState, onIntent: (BattleIntent) -> Unit) {
    val dialog = state.dialog ?: return
    val title = when (dialog) {
        is BattleDialog.Add -> R.string.add_participant
        is BattleDialog.EditHp -> R.string.edit_hp
        BattleDialog.Finish -> R.string.finish_question
    }
    val submitLabel = when (dialog) {
        is BattleDialog.Add -> R.string.add
        is BattleDialog.EditHp -> R.string.save
        BattleDialog.Finish -> R.string.finish_confirm
    }

    AlertDialog(
        onDismissRequest = { onIntent(BattleIntent.DismissDialog) },
        title = { Text(stringResource(title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (dialog) {
                    is BattleDialog.Add -> {
                        BattleField(dialog.name, R.string.name_label, state.saving, error = dialog.error == InputError.Name) {
                            onIntent(BattleIntent.ChangeName(it))
                        }
                        BattleField(dialog.hp, R.string.hp_label, state.saving, KeyboardType.Number, dialog.error == InputError.Hp) {
                            onIntent(BattleIntent.ChangeHp(it))
                        }
                        BattleField(dialog.initiative, R.string.initiative_label, state.saving, error = dialog.error == InputError.Initiative) {
                            onIntent(BattleIntent.ChangeInitiative(it))
                        }
                        dialog.error?.let {
                            SaveError(stringResource(when (it) {
                                InputError.Name -> R.string.input_name_error
                                InputError.Hp -> R.string.input_hp_error
                                InputError.Initiative -> R.string.input_initiative_error
                            }))
                        }
                    }
                    is BattleDialog.EditHp -> {
                        Text(dialog.name)
                        BattleField(dialog.hp, R.string.hp_label, state.saving, KeyboardType.Number, dialog.invalidHp) {
                            onIntent(BattleIntent.ChangeHp(it))
                        }
                        if (dialog.invalidHp) SaveError(stringResource(R.string.input_hp_error))
                    }
                    BattleDialog.Finish -> Text(stringResource(R.string.finish_message))
                }
                if (state.error == BattleError.Save) SaveError()
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onIntent(BattleIntent.SubmitDialog) },
                enabled = !state.saving,
            ) {
                Text(stringResource(submitLabel))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onIntent(BattleIntent.DismissDialog) },
                enabled = !state.saving,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun BattleField(
    value: String,
    label: Int,
    saving: Boolean,
    keyboard: KeyboardType = KeyboardType.Text,
    error: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !saving,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
internal fun SaveError(
    text: String = stringResource(R.string.save_error),
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, color = MaterialTheme.colorScheme.error)
}
