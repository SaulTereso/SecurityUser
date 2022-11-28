package com.security.securityuser.providers

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.security.securityuser.models.User
import java.io.File

class UserProvider {

    val db = Firebase.firestore.collection("Users")
    var storage = FirebaseStorage.getInstance().getReference().child("profile")

    fun create(user: User): Task<Void>{
        return db.document(user.id!!).set(user)
    }

    fun uploadImage(id: String, file: File): StorageTask<UploadTask.TaskSnapshot> {
        var fromFile = Uri.fromFile(file)
        val ref = storage.child("$id.jpg")
        storage = ref
        val uploadTask = ref.putFile(fromFile)

        return uploadTask.addOnFailureListener {
            Log.d("STORAGE", "ERROR: ${it.message}")
        }
    }

    fun getImageUrl(): Task<Uri> {
        return storage.downloadUrl
    }

    fun getUser(idUser: String): Task<DocumentSnapshot>{
        return db.document(idUser).get()
    }

    fun update(user: User): Task<Void>{
        val map: MutableMap<String, Any> = HashMap()
        map["name"] = user?.name!!
        map["app"] = user?.app!!
        map["phone"] = user?.phone!!
        map["image"] = user?.image!!
        return db.document(user?.id!!).update(map)
    }

}