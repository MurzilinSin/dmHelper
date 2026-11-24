package io.github.murzilinsin.dmhelper.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.murzilinsin.dmhelper.domain.Battle
import io.github.murzilinsin.dmhelper.domain.Participant
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun BattleScreen(state: BattleUiState, onIntent: (BattleIntent) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight()) {
                when {
                    state.loading -> CenteredContent { CircularProgressIndicator() }

                    state.error == BattleError.Load -> CenteredContent {
                        SaveError(stringResource(R.string.load_error))
                        TextButton(onClick = { onIntent(BattleIntent.Retry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }

                    state.battle == null -> CenteredContent {
                        Text(
                            stringResource(R.string.no_active_battle),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (state.error == BattleError.Save) SaveError()
                        Button(
                            onClick = { onIntent(BattleIntent.Create) },
                            enabled = !state.saving,
                        ) {
                            Text(stringResource(R.string.create_battle))
                        }
                    }

                    else -> BattleContent(
                        battle = state.battle,
                        enabled = !state.saving,
                        showSaveError = state.error == BattleError.Save && state.dialog == null,
                        onIntent = onIntent,
                    )
                }
            }
        }
        BattleDialogs(state, onIntent)
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun BattleContent(
    battle: Battle,
    enabled: Boolean,
    showSaveError: Boolean,
    onIntent: (BattleIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.battle_title), style = MaterialTheme.typography.titleLarge)
                val activeName = battle.participants.find { it.id == battle.activeParticipantId }?.name
                val subtitle = if (activeName == null) {
                    stringResource(R.string.preparation)
                } else {
                    stringResource(R.string.round, battle.round) + " · " +
                        stringResource(R.string.active_turn, activeName)
                }
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val addLabel = stringResource(R.string.add_participant)
            FilledTonalIconButton(
                onClick = { onIntent(BattleIntent.OpenAdd) },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = addLabel },
            ) {
                Text("+", fontSize = 26.sp)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (battle.participants.isEmpty()) {
                Text(
                    stringResource(R.string.empty_roster),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Roster(
                    participants = battle.participants,
                    activeId = battle.activeParticipantId,
                    enabled = enabled,
                    onIntent = onIntent,
                )
            }
        }

        if (showSaveError) SaveError(modifier = Modifier.padding(top = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onIntent(BattleIntent.ConfirmFinish) },
                enabled = enabled,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(stringResource(R.string.finish_battle))
            }
            Button(
                onClick = {
                    onIntent(if (battle.round == 0) BattleIntent.Start else BattleIntent.NextTurn)
                },
                enabled = enabled && battle.participants.isNotEmpty(),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                val label = if (battle.round == 0) R.string.start_battle else R.string.next_turn
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun Roster(
    participants: List<Participant>,
    activeId: Long?,
    enabled: Boolean,
    onIntent: (BattleIntent) -> Unit,
) {
    var preview by remember { mutableStateOf<List<Participant>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        preview?.let { order ->
            preview = order.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
    }

    LaunchedEffect(participants, draggingId, enabled) {
        if (draggingId == null && enabled) preview = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(preview ?: participants, key = { _, participant -> participant.id }) { index, participant ->
            ReorderableItem(reorderState, key = participant.id) { isDragging ->
                val dragEnabled = enabled && participants.size > 1
                val dragLabel = stringResource(R.string.reorder_participant, participant.name)
                val moveUp = stringResource(R.string.move_up)
                val moveDown = stringResource(R.string.move_down)
                ParticipantRow(
                    participant = participant,
                    active = participant.id == activeId,
                    isDragging = isDragging,
                    enabled = enabled,
                    onEditHp = { onIntent(BattleIntent.OpenEditHp(participant.id)) },
                    handle = Modifier
                        .size(48.dp)
                        .testTag("drag-${participant.id}")
                        .longPressDraggableHandle(
                            enabled = dragEnabled,
                            onDragStarted = {
                                draggingId = participant.id
                                preview = participants
                            },
                            onDragStopped = {
                                val toIndex = preview?.indexOfFirst { it.id == participant.id }
                                val original = participants.indexOfFirst { it.id == participant.id }
                                if (draggingId == participant.id && toIndex != null &&
                                    toIndex >= 0 && toIndex != original
                                ) {
                                    onIntent(BattleIntent.Move(participant.id, toIndex))
                                }
                                draggingId = null
                            },
                        )
                        .semantics {
                            contentDescription = dragLabel
                            customActions = buildList {
                                if (dragEnabled && index > 0) add(CustomAccessibilityAction(moveUp) {
                                    onIntent(BattleIntent.Move(participant.id, index - 1))
                                    true
                                })
                                if (dragEnabled && index < participants.lastIndex) {
                                    add(CustomAccessibilityAction(moveDown) {
                                        onIntent(BattleIntent.Move(participant.id, index + 1))
                                        true
                                    })
                                }
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant,
    active: Boolean,
    isDragging: Boolean,
    enabled: Boolean,
    onEditHp: () -> Unit,
    handle: Modifier,
) {
    val color = MaterialTheme.colorScheme
    val initiativeDescription = stringResource(R.string.initiative_description, participant.initiative)
    val activeDescription = stringResource(R.string.current_turn)
    val hpDescription = stringResource(R.string.hp_description, participant.name, participant.hp)
    Surface(
        modifier = Modifier.fillMaxWidth().semantics {
            if (active) stateDescription = activeDescription
        },
        shape = MaterialTheme.shapes.large,
        color = when {
            active -> color.primaryContainer
            isDragging -> color.surfaceContainerHigh
            else -> color.surfaceContainer
        },
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.widthIn(min = 38.dp),
                shape = MaterialTheme.shapes.small,
                color = color.surface,
            ) {
                Text(
                    participant.initiative.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        .semantics { contentDescription = initiativeDescription },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                participant.name,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = onEditHp,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = hpDescription },
            ) {
                Text(stringResource(R.string.hp_value, participant.hp))
            }
            Canvas(handle) {
                val halfWidth = 7.dp.toPx()
                for (offset in listOf(-3.dp, 3.dp)) {
                    val y = center.y + offset.toPx()
                    drawLine(
                        color = color.onSurfaceVariant,
                        start = Offset(center.x - halfWidth, y),
                        end = Offset(center.x + halfWidth, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
