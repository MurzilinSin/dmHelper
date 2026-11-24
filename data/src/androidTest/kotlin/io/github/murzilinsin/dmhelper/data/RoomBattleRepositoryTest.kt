package io.github.murzilinsin.dmhelper.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.murzilinsin.dmhelper.domain.BattleAction
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomBattleRepositoryTest {
    @Test
    fun restoresTheExactStateAfterClosingAndReopeningTheSQLiteFile() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "combat-${UUID.randomUUID()}.db"
        try {
            val before = withDatabase(context, name) { database ->
                val repository = RoomBattleRepository(database)
                assertNull(repository.load())
                repository.apply(BattleAction.Create)
                repository.apply(BattleAction.Add("Гоблин", 18, 19))
                repository.apply(BattleAction.Add("Лира", 12, 14))
                repository.apply(BattleAction.Move(participantId = 1L, toIndex = 1))
                repository.apply(BattleAction.Start)
                repository.apply(BattleAction.NextTurn)
                repository.apply(BattleAction.SetHp(participantId = 1L, hp = 4))
            }

            withDatabase(context, name) { database ->
                val repository = RoomBattleRepository(database)
                val restored = requireNotNull(repository.load())
                assertEquals(before, restored)
                assertEquals(listOf("Лира", "Гоблин"), restored.participants.map { it.name })
                assertEquals(listOf(14, 19), restored.participants.map { it.initiative })
                assertEquals(4, restored.participants.last().hp)
                assertEquals(1L, restored.activeParticipantId)
                assertEquals(1, restored.round)
                repository.apply(BattleAction.Finish)
            }

            withDatabase(context, name) { database ->
                assertNull(RoomBattleRepository(database).load())
                assertEquals(0, database.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM participant").use { cursor ->
                        cursor.moveToFirst()
                        cursor.getInt(0)
                    })
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private inline fun <T> withDatabase(
        context: Context,
        name: String,
        block: (BattleDatabase) -> T,
    ): T {
        val database = Room.databaseBuilder(context, BattleDatabase::class.java, name).build()
        return try {
            block(database)
        } finally {
            database.close()
        }
    }
}
