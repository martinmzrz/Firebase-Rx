package com.firefly.firebase_rx

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import io.reactivex.Completable
import io.reactivex.Single
import java.io.InputStream

class FireStorage(bucketUrl: String = ""){

    private val storage = if (bucketUrl.isBlank()){
        Firebase.storage
    } else {
        Firebase.storage(bucketUrl)
    }


    fun createReference(pathString: String): StorageReference{
        return storage.reference.child(pathString)
    }

    companion object {
        /**
         * Obtiene la Uri de una referencia al almacenamiento de Firebase.
         * En caso de que la referencia no exista se retorna una uri vacía.
         */
        fun StorageReference.getDownloadUri(): Single<Uri> {
            return Single.create { emitter ->
                val downloadTask = this.downloadUrl

                downloadTask.addOnSuccessListener {
                    if (it == null) {
                        emitter.onSuccess(Uri.EMPTY)
                    } else {
                        emitter.onSuccess(it)
                    }
                }.addOnFailureListener {
                    if (it is StorageException && it.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        emitter.onError(DataDoesNotExistsException())
                    } else {
                        emitter.onError(it)
                    }
                }
            }
        }

        fun StorageReference.uploadBytes(inputStream: InputStream): Completable {
            return Completable.create { emitter ->

                val uploadTask = this.putStream(inputStream)

                uploadTask.addOnCompleteListener {
                    emitter.onComplete()
                }.addOnFailureListener {
                    emitter.onError(it)
                }
            }
        }

        fun StorageReference.uploadBytes(byteArray: ByteArray, metadata: Map<String, String>? = null): Completable {
            return Completable.create { emitter ->

                val uploadTask = if(metadata == null || metadata.isEmpty()){
                    this.putBytes(byteArray)
                } else{
                    val metadataBuilder = StorageMetadata.Builder()

                    metadata.forEach{(k, v) ->
                        metadataBuilder.setCustomMetadata(k, v)
                    }

                    this.putBytes(byteArray, metadataBuilder.build())
                }

                uploadTask.addOnCompleteListener {
                    emitter.onComplete()
                }.addOnFailureListener {
                    emitter.onError(it)
                }
            }
        }
    }
}