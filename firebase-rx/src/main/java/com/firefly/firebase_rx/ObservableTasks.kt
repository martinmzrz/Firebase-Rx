package com.firefly.firebase_rx

import com.google.android.gms.tasks.Task
import io.reactivex.Single

class ObservableTasks {

    companion object{
        fun <T> Task<T>.toSingle(): Single<T> {
            return Single.create { emitter ->
                this.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result == null) {
                            emitter.onError(NullPointerException())
                        } else {
                            emitter.onSuccess(task.result!!)
                        }
                    } else {
                        if (task.exception == null) {
                            emitter.onError(Exception("Error"))
                        } else {
                            emitter.onError(task.exception!!)
                        }
                    }
                }
            }
        }
    }
}