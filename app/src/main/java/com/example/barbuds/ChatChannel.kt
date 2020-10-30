package com.example.barbuds

data class ChatChannel(val userIds: MutableList<String> ) {
    constructor(): this(mutableListOf())
}