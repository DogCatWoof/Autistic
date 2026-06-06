package org.meow.autistic.di

import org.koin.dsl.module
import org.meow.autistic.data.firestore.FirestoreDaos
import org.meow.autistic.data.firestore.FirestoreSource
import org.meow.autistic.data.firestore.FirestoreSyncService
import org.meow.autistic.data.mood.MoodDao
import org.meow.autistic.data.note.NoteDao
import org.meow.autistic.data.task.TaskDao

val firestoreModule = module {
    single { FirestoreSource() }
    single {
        FirestoreSyncService(
            source = get(),
            daos = FirestoreDaos(
                task = get<TaskDao>(),
                note = get<NoteDao>(),
                mood = get<MoodDao>(),
            ),
        )
    }
}
