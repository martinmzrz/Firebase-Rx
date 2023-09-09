package com.firefly.firebase_rx

import com.google.firebase.database.DataSnapshot

class DataDoesNotExistsException(var dataSnapshot: DataSnapshot? = null): Exception("The requested data does not exist")