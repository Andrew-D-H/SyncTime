package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notifications = mutableListOf<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)

        // initial data
        notifications.clear()
        notifications.addAll(dummyNotifications())  // dummy data for now, replace with actual data

        adapter = NotificationsAdapter(
            items = notifications,
            onItemClick = { notif, position ->
                // Mark as read when tapped
                if (!notif.isRead) {
                    notif.isRead = true
                    adapter.notifyItemChanged(position)
                }
                // TODO: navigate to detail if you have a detail screen
            },
            onMoreClick = { notif, anchorView ->
                showNotificationMenu(notif, anchorView)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    // on more (dots) click
    private fun showNotificationMenu(notification: Notification, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_notification_item, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_mark_unread -> {
                    notification.isRead = false
                    val index = notifications.indexOfFirst { it.id == notification.id }
                    if (index != -1) adapter.notifyItemChanged(index)
                    true
                }
                R.id.action_delete -> {
                    val index = notifications.indexOfFirst { it.id == notification.id }
                    if (index != -1) {
                        notifications.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // Temporary fake data so you can see the UI
    private fun dummyNotifications(): List<Notification> = listOf(
        Notification(
            id = 1,
            avatarRes = R.drawable.baseline_account_circle_24,
            title = "Gandalf the Grey has arrived at the Shire",
            meta = "Updates • 5 min ago",
            isRead = false
        ),
        Notification(
            id = 2,
            avatarRes = R.drawable.baseline_account_circle_24,
            title = "Harry Potter added you to a group",
            meta = "Groups • 28 min ago",
            isRead = false
        ),
        Notification(
            id = 3,
            avatarRes = R.drawable.baseline_account_circle_24,
            title = "Frank Reynolds added you to a group",
            meta = "Groups • 12 hours ago",
            isRead = true
        ),
        Notification(
            id = 4,
            avatarRes = R.drawable.baseline_account_circle_24,
            title = "Dr. Doofenshmirtz accepted your friend request",
            meta = "Friends • 1 week ago",
            isRead = true
        )
    )
}

