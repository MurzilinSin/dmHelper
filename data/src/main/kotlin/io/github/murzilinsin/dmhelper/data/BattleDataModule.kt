package io.github.murzilinsin.dmhelper.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.murzilinsin.dmhelper.domain.BattleRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BattleDataModule {
    @Binds
    fun repository(implementation: RoomBattleRepository): BattleRepository

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): BattleDatabase =
            Room.databaseBuilder(context, BattleDatabase::class.java, "dmhelper.db").build()
    }
}
