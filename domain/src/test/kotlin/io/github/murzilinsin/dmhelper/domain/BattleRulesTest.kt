package io.github.murzilinsin.dmhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BattleRulesTest {
    @Test
    fun equalInitiativesKeepEntryOrderAndManualMovesAcrossInitiativesKeepTheirValues() {
        var battle = preparedBattle()
        assertEquals(listOf("Гоблин", "Страж", "Лира"), battle.participants.map { it.name })

        battle = update(battle, BattleAction.Move(participantId = 3, toIndex = 0))
        battle = update(battle, BattleAction.Move(participantId = 1, toIndex = 2))
        assertEquals(listOf("Лира", "Страж", "Гоблин"), battle.participants.map { it.name })
        assertEquals(listOf(14, 18, 18), battle.participants.map { it.initiative })
    }

    @Test
    fun nextTurnUsesTheManualOrderAndKeepsTheActiveParticipantDuringReordering() {
        var battle = update(preparedBattle(), BattleAction.Start)
        val initialActive = battle.activeParticipantId

        battle = update(battle, BattleAction.Move(participantId = 3, toIndex = 1))
        assertEquals(initialActive, battle.activeParticipantId)
        assertEquals(1, battle.round)

        battle = update(battle, BattleAction.NextTurn)
        assertEquals(3L, battle.activeParticipantId)
        battle = update(battle, BattleAction.NextTurn)
        assertEquals(2L, battle.activeParticipantId)
        battle = update(battle, BattleAction.NextTurn)
        assertEquals(initialActive, battle.activeParticipantId)
        assertEquals(2, battle.round)
    }

    @Test
    fun addingAParticipantKeepsTheRelativeOrderOfExistingParticipants() {
        var battle = update(preparedBattle(), BattleAction.Move(participantId = 3, toIndex = 0))
        battle = update(battle, BattleAction.Add("Волк", 6, 17))
        assertEquals(listOf("Волк", "Лира", "Гоблин", "Страж"), battle.participants.map { it.name })
    }

    @Test
    fun createIsIdempotentAndFinishingAllowsANewEmptyBattle() {
        val battle = preparedBattle()
        assertEquals(battle, update(battle, BattleAction.Create))
        assertNull(BattleRules.apply(battle, BattleAction.Finish))
        assertEquals(Battle(), BattleRules.apply(null, BattleAction.Create))
        assertEquals(Battle(), update(Battle(), BattleAction.Start))
    }

    private fun preparedBattle(): Battle {
        val first = update(Battle(), BattleAction.Add("Гоблин", 4, 18))
        val second = update(first, BattleAction.Add("Страж", 8, 18))
        return update(second, BattleAction.Add("Лира", 15, 14))
    }

    private fun update(battle: Battle, action: BattleAction): Battle =
        requireNotNull(BattleRules.apply(battle, action))
}
