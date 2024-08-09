package com.alexander_treml.asttuner

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

val Context.tuningDataStore: DataStore<StoredTunings> by dataStore(
    fileName = "tunings.pb",
    serializer = Tunings
)

object Tunings : Serializer<StoredTunings> {
    val standard = listOf(
        Note.new(-5), // E2
        Note.new(0),  // A2
        Note.new(5),  // D3
        Note.new(10), // G3
        Note.new(14), // B3
        Note.new(19)  // E4
    )

    suspend fun loadTunings(context: Context): Map<String, List<Note>> {
        return context.tuningDataStore.data.first().tuningsMap.mapValues { entry ->
            entry.value.noteList.map { note -> Note.new(note) }
        }
    }

    // This launches a work request that will be completed even after process death
    fun saveTunings(context: Context, tunings: Map<String, List<Note>>) {
        val saveData = Data.Builder().putAll(tunings.mapValues { entry ->
            // Needs to be int array to be accepted by the "Data" type
            entry.value.map { note -> note.distanceToReference }.toTypedArray()
        }).build()
        val worker = OneTimeWorkRequestBuilder<SaveWorker>()
            .setInputData(saveData)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
    }

    class SaveWorker(private val context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            try {
                context.tuningDataStore.updateData { current ->
                    current.toBuilder().clear().putAllTunings(
                        (inputData.keyValueMap as Map<String, *>).mapValues { entry ->
                            @Suppress("UNCHECKED_CAST")
                            StoredNotes.newBuilder()
                                .addAllNote((entry.value as Array<Int>).asIterable())
                                .build()
                        }
                    ).build()
                }
                return Result.success()
            } catch (e: Exception) {
                return Result.failure()
            }
        }
    }

    override val defaultValue: StoredTunings
        get() = StoredTunings.newBuilder()
            .putTunings(
                "Six String Standard",
                StoredNotes.newBuilder().addAllNote(listOf(-5, 0, 5, 10, 14, 19)).build()
            ).build()

    override suspend fun readFrom(input: InputStream): StoredTunings {
        try {
            return StoredTunings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: StoredTunings,
        output: OutputStream
    ) = t.writeTo(output)
}

