package com.firefly.firebase_rx

/**
 * Representa un cambio que ocurrio en un hijo de una consulta de la base de datos.
 */
data class ChildEvent<T>(val operation: Int, val item: T) {
    companion object{
        const val DELETED = 1
        const val ADDED = 2
        const val CHANGED = 3
        const val MOVED = 4
    }
}