package com.example.barbuds;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import firestream.chat.firestore.FirestoreService;
import firestream.chat.namespace.Fire;

public class Chat extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Fire.stream().initialize(this, new FirestoreService());
    }
}