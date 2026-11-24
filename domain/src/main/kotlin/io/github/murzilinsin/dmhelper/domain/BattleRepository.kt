package io.github.murzilinsin.dmhelper.domain

interface BattleRepository {
    suspend fun load(): Battle?
    suspend fun apply(action: BattleAction): Battle?
}
