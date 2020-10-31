package com.example.barbuds.util

import com.example.barbuds.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.jetbrains.anko.support.v4.toast
import kotlin.NullPointerException

object FirestoreUtil {


    //lazy means we will only get data once we need it apparently
    private val firestoreInstance : FirebaseFirestore by lazy {FirebaseFirestore.getInstance()}
    private val currentUserDocRef: DocumentReference
    get() = firestoreInstance.document("Users/${FirebaseAuth.getInstance().currentUser?.uid?:throw NullPointerException("UID is null")}")

    private val chatChannelsCollectionRef = firestoreInstance.collection("chatChannels")

    fun initCurrentUserIfFirstTime(onComplete:()->Unit){
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot -> if (!documentSnapshot.exists()){
            val newUser = User(FirebaseAuth.getInstance().currentUser?.displayName?:"", "",null)
            currentUserDocRef.set(newUser).addOnSuccessListener {
                onComplete() }
            }
        else
            onComplete()
        }

    }

    fun updateCurrentUser(name:String = "", bio:String ="", profilePicturePath:String? =null){
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val userFieldMap = mutableMapOf<String,Any>()
        if (name.isNotBlank()) userFieldMap["name"] = name
        if (bio.isNotBlank()) userFieldMap["bio"] = bio
        if (profilePicturePath!=null) userFieldMap["profilePicturePath"] = profilePicturePath
        if (uid != null) {
            firestoreInstance.collection("Users").document(uid).set(userFieldMap)
        }
        currentUserDocRef.update(userFieldMap)
    }

    fun getCurrentUser(onComplete: (User) -> Unit){
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot -> if (!documentSnapshot.exists()) {
            val newUser = User(FirebaseAuth.getInstance().currentUser?.displayName ?: "", "", null)
            currentUserDocRef.set(newUser).addOnSuccessListener {

                }
            }
        }
        currentUserDocRef.get().addOnSuccessListener { it.toObject(User::class.java)?.let { it1 -> onComplete(it1) } }
    }
}