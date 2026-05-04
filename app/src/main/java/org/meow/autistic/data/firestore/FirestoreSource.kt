package org.meow.autistic.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Thin wrapper around the Firestore SDK.
 *
 * All paths rooted at `users/{uid}/{collection}/{docId}`.
 * All methods run on [Dispatchers.IO].
 *
 * @param firestore Firestore instance; override in tests.
 */
class FirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    private fun colRef(uid: String, collection: String) =
        firestore.collection("users").document(uid).collection(collection)

    suspend fun upsert(uid: String, collection: String, docId: String, data: Map<String, Any?>) =
        withContext(Dispatchers.IO) {
            colRef(uid, collection).document(docId).set(data).await()
        }

    suspend fun delete(uid: String, collection: String, docId: String) =
        withContext(Dispatchers.IO) {
            colRef(uid, collection).document(docId).delete().await()
        }

    suspend fun fetchAll(uid: String, collection: String): QuerySnapshot =
        withContext(Dispatchers.IO) {
            colRef(uid, collection).get().await()
        }

    suspend fun fetchSince(uid: String, collection: String, since: Instant): QuerySnapshot =
        withContext(Dispatchers.IO) {
            colRef(uid, collection)
                .whereGreaterThan("lastModifiedAt", Timestamp(since.epochSecond, since.nano))
                .get()
                .await()
        }
}
