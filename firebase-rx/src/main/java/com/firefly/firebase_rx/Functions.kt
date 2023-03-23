package com.firefly.firebase_rx

import com.firefly.logutils.logD
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Convierte un [String] a una [DatabaseReference] de Firebase.
 * @param url Ruta de inicializacion de la base de datos, nulo para referencia default.
 */
fun String.toDatabaseReference(url: String = ""): DatabaseReference {
    val database = if(url.isBlank())
        Firebase.database.reference
    else
        Firebase.database(url).reference

    return database.child(this)
}

/**
 * Guarda un valor en la ruta de la base de datos.
 *
 * @param value nuevo valor para la ruta de la base de datos.
 * @param tag parametro opcional para imprimir un log cuando el proceso finalice.
 */
fun DatabaseReference.rxSetValue(value: Any, tag: String? = null): Completable {
    return Completable.create { emitter ->
        this.setValue(value).addOnCompleteListener { task ->
            if (!tag.isNullOrBlank()) {
                "update completable with $tag completed with result: ${task.isSuccessful}".logD()
            }

            if (task.isSuccessful) {
                emitter.onComplete()
            } else {
                if (task.exception == null) {
                    emitter.onError(Exception("Error in completable ${if(tag.isNullOrBlank()) "" else "with tag = $tag"}"))
                } else {
                    emitter.onError(task.exception!!)
                }
            }
        }
    }
}

/**
 * Elimina el valor de la ruta de la base de datos.
 *
 * @param tag parametro opcional para imprimir un log cuando el proceso finalice.
 */
fun DatabaseReference.rxRemoveValue(tag: String? = null): Completable {
    return Completable.create { emitter ->
        this.removeValue().addOnCompleteListener { task ->

            if (!tag.isNullOrBlank()) {
                "Remove completable with $tag completed with result: ${task.isSuccessful}".logD()
            }

            if (task.isSuccessful) {
                emitter.onComplete()
            } else {
                if (task.exception == null) {
                    emitter.onError(Exception("Error in completable ${if(tag.isNullOrBlank()) "" else "with tag = $tag"}"))
                } else {
                    emitter.onError(task.exception!!)
                }
            }
        }
    }
}

/**
 * Observa sobre una [Query] los cambios que ocurran en los nodos hijos.
 *
 * El Observable se mantiene escuchando hasta que se llama al metodo dispose.
 */
fun Query.toRxObservableChildren(): Observable<ChildEvent<DataSnapshot>> {
    var childListener: ChildEventListener? = null
    val observable = Observable.create<ChildEvent<DataSnapshot>> { emitter ->
        childListener = object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {
                emitter.onError(p0.toException())
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                emitter.onNext(ChildEvent(ChildEvent.MOVED, p0))
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                emitter.onNext(ChildEvent(ChildEvent.CHANGED, p0))
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                emitter.onNext(ChildEvent(ChildEvent.ADDED, p0))
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                emitter.onNext(ChildEvent(ChildEvent.DELETED, p0))
            }
        }
        this.addChildEventListener(childListener!!)
    }

    return observable.doOnDispose {
        childListener?.let { this.removeEventListener(it) }
    }
}

/**
 * Convierte una [Query] a [Single] de RX
 */
fun Query.toRxSingle(): Single<DataSnapshot> {
    var valueEventListener: ValueEventListener? = null

    return Single.create<DataSnapshot> { emitter ->

        valueEventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                emitter.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists())
                    emitter.onSuccess(p0)
                else
                    emitter.onError(DataDoesNotExistsException())
            }
        }
        valueEventListener?.let<ValueEventListener, Unit> { addListenerForSingleValueEvent(it) }
    }.doOnDispose {
        valueEventListener?.let { removeEventListener(it) }
    }
}

fun <T> Task<T>.toRxMaybe(): Maybe<T> {
    return Maybe.create { emitter ->
        this.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result == null) {
                    emitter.onComplete()
                } else {
                    emitter.onSuccess(task.result)
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

/**
 * Observa sobre una [Query] los hijos existentes y completa el observable al finalizar la lista.
 */
fun Query.toRxObservableChildrenSingleEvent(): Observable<DataSnapshot> {
    var valueEventListener: ValueEventListener? = null

    return Observable.create { emitter ->

        valueEventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                emitter.onError(p0.toException())
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    emitter.onNext(it)
                }

                emitter.onComplete()
            }
        }

        valueEventListener?.let<ValueEventListener, Unit> { addListenerForSingleValueEvent(it) }
    }.doOnDispose {
        valueEventListener?.let { removeEventListener(it) }
    }
}

/**
 * Actualiza una lista de nodos de la BD.
 *
 * @param update mapa de nodos y valores a actualizar.
 */
fun DatabaseReference.rxUpdateChildren(update: Map<String, Any>): Completable {
    return Completable.create { emitter ->
        this.updateChildren(update).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                emitter.onComplete()
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