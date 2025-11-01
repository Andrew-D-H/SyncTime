package com.example.synctime

data class ChatMessage(
    val senderId: String? = null,
    val message: String? = null,
    val timestamp: Long? = null
)

data class ChatRoom(
    val chatId: String? = null,
    val name: String? = null,
    val type: String? = "private", // "private" or "group"
    val members: Map<String, Boolean>? = null
)


// Future code once chat UI is done

/* -- Create/join chat --
private val db = FirebaseDatabase.getInstance().getReference("chats")

fun createGroupChat(groupName: String, memberIds: List<String>): String {
    val chatId = db.push().key ?: return ""

    val chatRoom = ChatRoom(
        chatId = chatId,
        name = groupName,
        type = "group",
        members = memberIds.associateWith { true }
    )

    db.child(chatId).setValue(chatRoom)
    return chatId
}

fun createPrivateChat(user1: String, user2: String): String {
    val chatId = db.push().key ?: return ""
    val chatRoom = ChatRoom(
        chatId = chatId,
        type = "private",
        members = mapOf(user1 to true, user2 to true)
    )
    db.child(chatId).setValue(chatRoom)
    return chatId
}
*/

/* -- Send messages --
fun sendMessage(chatId: String, senderId: String, messageText: String) {
    val message = ChatMessage(
        senderId = senderId,
        message = messageText,
        timestamp = System.currentTimeMillis()
    )

    db.child(chatId).child("messages").push().setValue(message)
}
*/

/* -- listener --
fun listenForMessages(chatId: String, onMessagesUpdate: (List<ChatMessage>) -> Unit) {
    val messagesRef = db.child(chatId).child("messages")

    messagesRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val messages = mutableListOf<ChatMessage>()
            for (msgSnap in snapshot.children) {
                msgSnap.getValue(ChatMessage::class.java)?.let { messages.add(it) }
            }
            onMessagesUpdate(messages.sortedBy { it.timestamp })
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error: ${error.message}")
        }
    })
}
*/

/* -- fetch chats based on user --
fun fetchUserChats(userId: String, onResult: (List<ChatRoom>) -> Unit) {
    db.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val chatList = mutableListOf<ChatRoom>()
            for (chatSnap in snapshot.children) {
                val chatRoom = chatSnap.getValue(ChatRoom::class.java)
                if (chatRoom?.members?.containsKey(userId) == true) {
                    chatList.add(chatRoom)
                }
            }
            onResult(chatList)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error: ${error.message}")
        }
    })
}

*/

/* To-do list
* make ChatAdapter class
* make xml for messages
* make class activity for chat
* */