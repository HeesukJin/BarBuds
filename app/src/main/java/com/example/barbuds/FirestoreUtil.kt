package com.example.barbuds

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.NullPointerException

object FirestoreUtil {

    private val firestoreInstance : FirebaseFirestore by lazy {FirebaseFirestore.getInstance()}
    private val currentUserDocRef: DocumentReference
    get() = firestoreInstance.document("users/${FirebaseAuth.getInstance().currentUser?.uid?:throw NullPointerException("UID is null")}")

    private val chatChannelsCollectionRef = firestoreInstance.collection("chatChannels")

    fun initCurrentUserIfFirstTime(onComplete:()->Unit){
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot -> if (!documentSnapshot.exists()){
            val newUser = use
            }
        }
    }
}