package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.core.sync.DiaryPhotoSyncScheduler
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType
import com.abhinavsirohi.kiwi.data.local.DiaryPhotoLocalStore
import com.abhinavsirohi.kiwi.data.local.StoredDiaryPhoto
import com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.RoomDiaryPhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDiaryPhotoRepositoryTest {
    private lateinit var database: KiwiDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            KiwiDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun addAndDeletePhoto_areLocalFirstAndQueued() = runBlocking {
        database.diaryDao().upsert(
            DiaryEntryEntity(
                localId = "entry-1",
                title = "A day",
                content = "Writing",
                entryDate = "2026-07-14",
                syncMetadata = SyncMetadata(userId = "user-1", createdAt = 1000, updatedAt = 1000, deviceId = "device-1"),
            ),
        )
        val localStore = FakeLocalStore()
        var scheduled = 0
        var timestamp = 2000L
        val repository = RoomDiaryPhotoRepository(
            database = database,
            sessionProvider = object : SessionProvider {
                override fun currentSession() = RemoteResult.Success(AuthenticatedSession("user-1", "user@example.com"))
            },
            deviceId = "device-1",
            localStore = localStore,
            syncScheduler = DiaryPhotoSyncScheduler { scheduled++ },
            now = { timestamp },
            newLocalId = { "photo-1" },
        )

        val added = repository.addPhoto("entry-1", "content://photo")

        assertTrue(added is AppResult.Success)
        val stored = database.diaryDao().findPhoto("photo-1")
        assertNotNull(stored)
        assertEquals("user-1/entry-1/photo-1.jpg", stored?.remotePath)
        assertEquals(SyncStatus.PENDING, stored?.syncMetadata?.syncStatus)
        val queuedUpload = database.pendingChangeDao().findById("DIARY_PHOTO:photo-1")
        assertEquals(SyncRecordType.DIARY_PHOTO, queuedUpload?.recordType)
        assertEquals(SyncOperation.UPSERT, queuedUpload?.operation)
        assertEquals(1, scheduled)

        timestamp = 3000L
        assertTrue(repository.deletePhoto("photo-1", timestamp) is AppResult.Success)
        assertEquals(3000L, database.diaryDao().findPhoto("photo-1")?.syncMetadata?.deletedAt)
        assertEquals(SyncOperation.DELETE, database.pendingChangeDao().findById("DIARY_PHOTO:photo-1")?.operation)
        assertEquals(2, scheduled)
        assertTrue(!localStore.deleted)
    }

    private class FakeLocalStore : DiaryPhotoLocalStore {
        var deleted = false
        override fun importCompressed(sourceUri: String, localId: String) =
            StoredDiaryPhoto("/private/$localId.jpg", 800, 600, 1234)
        override fun readBytes(path: String) = byteArrayOf(1)
        override fun delete(path: String): Boolean { deleted = true; return true }
    }
}
