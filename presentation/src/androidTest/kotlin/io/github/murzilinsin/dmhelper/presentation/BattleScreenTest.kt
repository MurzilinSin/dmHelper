package io.github.murzilinsin.dmhelper.presentation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BattleScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun buildsACombatAndRestoresTurnAndOrderAfterRotationAndActivityRecreation() {
        clearPreviousBattle()
        compose.onNodeWithText("Создать бой").performClick()
        addParticipant("Лира", "12", "14")
        addParticipant("Гоблин", "8", "18")

        assertAbove("Гоблин", "Лира")
        dragBelow(draggedId = 2, targetId = 1)
        assertAbove("Лира", "Гоблин")

        compose.onNodeWithText("Начать бой").performClick()
        compose.onNodeWithText("Раунд 1 · Ходит Лира").assertIsDisplayed()
        compose.onNodeWithText("Следующий ход").performClick()
        compose.onNodeWithContentDescription("ХП участника Гоблин. Текущее значение 8.").performClick()
        compose.onNodeWithText("ХП").performTextReplacement("5")
        compose.onNodeWithText("Сохранить").performClick()

        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            var landscape = false
            compose.activityRule.scenario.onActivity {
                landscape = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            landscape
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Раунд 1 · Ходит Гоблин").assertIsDisplayed()
        compose.onNodeWithText("5 ХП").assertIsDisplayed()
        assertAbove("Лира", "Гоблин")
    }

    private fun clearPreviousBattle() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Создать бой") or hasText("Завершить бой"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodes(hasText("Завершить бой")).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Завершить бой").performClick()
            compose.onNodeWithText("Завершить").performClick()
        }
    }

    private fun addParticipant(name: String, hp: String, initiative: String) {
        compose.onNodeWithContentDescription("Добавить участника").performClick()
        compose.onNodeWithText("Название").performTextInput(name)
        compose.onNodeWithText("ХП").performTextInput(hp)
        compose.onNodeWithText("Инициатива").performTextInput(initiative)
        compose.onNodeWithText("Добавить").performClick()
    }

    private fun assertAbove(first: String, second: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            val upper = compose.onAllNodes(hasText(first)).fetchSemanticsNodes().singleOrNull()
            val lower = compose.onAllNodes(hasText(second)).fetchSemanticsNodes().singleOrNull()
            upper != null && lower != null && upper.boundsInRoot.top < lower.boundsInRoot.top
        }
        assertTrue(
            compose.onNodeWithText(first).fetchSemanticsNode().boundsInRoot.top <
                compose.onNodeWithText(second).fetchSemanticsNode().boundsInRoot.top,
        )
    }

    private fun dragBelow(draggedId: Int, targetId: Int) {
        val dragged = compose.onNodeWithTag("drag-$draggedId")
        val target = compose.onNodeWithTag("drag-$targetId")
        val distance = target.fetchSemanticsNode().boundsInRoot.center.y -
            dragged.fetchSemanticsNode().boundsInRoot.center.y

        dragged.performTouchInput {
            down(center)
            advanceEventTime(700)
            repeat(10) {
                moveBy(Offset(0f, distance / 10), delayMillis = 25)
            }
            up()
        }
    }
}
