package com.example.synctime.repository

import com.example.synctime.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object NotificationRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun sendNotification(
        toUid: String,
        title: String,
        meta: String,
        type: String
    ) {
        val fromUid = auth.currentUser?.uid ?: return

        // Get sender profile URL
        db.child("users")
            .child(fromUid)
            .child("profileURL")
            .get()
            .addOnSuccessListener { snapshot ->

                val profileUrl = snapshot.getValue(String::class.java)

                val notifId = db.child("users")
                    .child(toUid)
                    .child("notifications")
                    .push()
                    .key ?: return@addOnSuccessListener

                val notif = Notification(
                    id = notifId,
                    senderId = fromUid,
                    senderProfileUrl = profileUrl,
                    title = title,
                    meta = meta,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                db.child("users")
                    .child(toUid)
                    .child("notifications")
                    .child(notifId)
                    .setValue(notif)
            }
    }
}
