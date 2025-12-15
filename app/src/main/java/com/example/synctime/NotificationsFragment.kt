package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notifications = mutableListOf<Notification>()

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var notificationsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)

        adapter = NotificationsAdapter(
            items = notifications,
            onItemClick = { notif, position ->
                if (!notif.isRead) {
                    // 1) Update local model immediately
                    notif.isRead = true
                    adapter.notifyItemChanged(position)

                    // 2) Persist to Firebase
                    markRead(notif.id, true)
                }
            },
            onMoreClick = { notif, anchorView ->
                showNotificationMenu(notif, anchorView)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        attachNotificationsListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detachNotificationsListener()
    }

    private fun notificationsRef(): DatabaseReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.child("users").child(uid).child("notifications")
    }

    private fun attachNotificationsListener() {
        val ref = notificationsRef() ?: return

        // If you want newest first using a query:
        val query = ref.orderByChild("timestamp")

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                snapshot.children.forEach { child ->
                    val notif = child.getValue(Notification::class.java)
                    if (notif != null) notifications.add(notif)
                }

                // orderByChild gives oldest->newest, reverse for newest at top
                notifications.sortByDescending { it.timestamp }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        query.addValueEventListener(notificationsListener as ValueEventListener)
    }

    private fun detachNotificationsListener() {
        val ref = notificationsRef() ?: return
        notificationsListener?.let { ref.removeEventListener(it) }
        notificationsListener = null
    }

    private fun markRead(notificationId: String, read: Boolean) {
        val ref = notificationsRef() ?: return
        ref.child(notificationId).child("isRead").setValue(read)
    }

    private fun deleteNotification(notificationId: String) {
        val ref = notificationsRef() ?: return
        ref.child(notificationId).removeValue()
    }

    private fun showNotificationMenu(notification: Notification, anchor: View) {
        val options = arrayOf("Mark as unread", "Delete")

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> markRead(notification.id, false)
                    1 -> deleteNotification(notification.id)
                }
            }
            .show()
    }
}
