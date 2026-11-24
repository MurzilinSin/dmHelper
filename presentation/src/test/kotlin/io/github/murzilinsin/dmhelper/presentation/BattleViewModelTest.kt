package io.github.murzilinsin.dmhelper.presentation

import io.github.murzilinsin.dmhelper.domain.Battle
import io.github.murzilinsin.dmhelper.domain.BattleAction
import io.github.murzilinsin.dmhelper.domain.BattleRepository
import io.github.murzilinsin.dmhelper.domain.BattleRules
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BattleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidFormStaysOpenAndRetryKeepsItsValuesAfterAWriteFailure() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = BattleViewModel(repository)
        advanceUntilIdle()
        viewModel.onIntent(BattleIntent.Create)
        advanceUntilIdle()
        viewModel.onIntent(BattleIntent.OpenAdd)
        viewModel.onIntent(BattleIntent.ChangeName("Лира"))
        viewModel.onIntent(BattleIntent.ChangeHp("-1"))
        viewModel.onIntent(BattleIntent.ChangeInitiative("-2"))
        viewModel.onIntent(BattleIntent.SubmitDialog)

        assertEquals(InputError.Hp, (viewModel.state.value.dialog as BattleDialog.Add).error)
        assertEquals(0, viewModel.state.value.battle?.participants?.size)

        viewModel.onIntent(BattleIntent.ChangeHp("12"))
        repository.failNext = true
        viewModel.onIntent(BattleIntent.SubmitDialog)
        advanceUntilIdle()

        assertEquals(BattleError.Save, viewModel.state.value.error)
        assertEquals("12", (viewModel.state.value.dialog as BattleDialog.Add).hp)
        assertEquals(0, viewModel.state.value.battle?.participants?.size)

        viewModel.onIntent(BattleIntent.SubmitDialog)
        advanceUntilIdle()

        assertNull(viewModel.state.value.dialog)
        assertNull(viewModel.state.value.error)
        assertEquals(-2, viewModel.state.value.battle?.participants?.single()?.initiative)
    }

    private class FakeRepository : BattleRepository {
        var failNext = false
        private var battle: Battle? = null

        override suspend fun load() = battle

        override suspend fun apply(action: BattleAction): Battle? {
            if (failNext) {
                failNext = false
                throw IOException()
            }
            battle = BattleRules.apply(battle, action)
            return battle
        }
    }
}
