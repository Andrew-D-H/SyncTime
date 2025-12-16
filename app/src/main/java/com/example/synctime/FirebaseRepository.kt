package com.example.synctime

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository class that handles all Firebase Realtime Database operations
 * for the SyncTime chat application.
 * 
 * Database Structure:
 * - users/{userId} - User profiles
 * - chats/{chatId} - Chat metadata (participants, last message, etc.)
 * - messages/{chatId}/{messageId} - Individual messages
 * - userChats/{userId}/{chatId} - Index of chats per user
 */
object FirebaseRepository {

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Database references
    private val usersRef: DatabaseReference by lazy { database.getReference("users") }
    private val chatsRef: DatabaseReference by lazy { database.getReference("chats") }
    private val messagesRef: DatabaseReference by lazy { database.getReference("messages") }
    private val userChatsRef: DatabaseReference by lazy { database.getReference("userChats") }

    // ==================== CURRENT USER ====================

    /**
     * Get the current logged-in user's ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get the current logged-in user's display name
     */
    fun getCurrentUserName(): String = auth.currentUser?.displayName ?: "Unknown"

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // ==================== USER OPERATIONS ====================

    /**
     * Save or update user profile in Firebase after login
     */
    suspend fun saveCurrentUser() {
        val firebaseUser = auth.currentUser ?: return
        
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "Unknown",
            profilePicUrl = firebaseUser.photoUrl?.toString() ?: ""
        )
        
        usersRef.child(firebaseUser.uid).setValue(user).await()
    }

    /**
     * Get a user by their ID
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all users (for starting new chats)
     */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (child in snapshot.children) {
                    child.getValue(User::class.java)?.let { user ->
                        // Exclude current user from the list
                        if (user.uid != getCurrentUserId()) {
                            users.add(user)
                        }
                    }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    // ==================== CHAT OPERATIONS ====================

    /**
     * Get all chats for the current user (real-time updates)
     */
    fun getUserChats(): Flow<List<Chat>> = callbackFlow {
        val currentUserId = getCurrentUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatIds = snapshot.children.mapNotNull { it.key }
                
                if (chatIds.isEmpty()) {
                    trySend(emptyList())
                    return
                }

                // Fetch each chat's details
                val chats = mutableListOf<Chat>()
                var fetchedCount = 0

                for (chatId in chatIds) {
                    chatsRef.child(chatId).get().addOnSuccessListener { chatSnapshot ->
                        chatSnapshot.getValue(Chat::class.java)?.let { chat ->
                            chats.add(chat)
                        }
                        fetchedCount++
                        
                        if (fetchedCount == chatIds.size) {
                            // Sort by timestamp (most recent first)
                            trySend(chats.sortedByDescending { it.timestamp })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        userChatsRef.child(currentUserId).addValueEventListener(listener)
        awaitClose { userChatsRef.child(currentUserId).removeEventListener(listener) }
    }

    /**
     * Create a new chat between current user and another user
     * Returns the chat ID
     */
    suspend fun createChat(otherUserId: String, otherUserName: String): String {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        
        // Check if chat already exists between these two users
        val existingChatId = findExistingChat(currentUserId, otherUserId)
        if (existingChatId != null) {
            return existingChatId
        }

        // Create new chat
        val chatId = chatsRef.push().key ?: throw Exception("Failed to create chat")
        
        val chat = Chat(
            id = chatId,
            name = otherUserName,  // For 1-on-1 chats, use the other user's name
            lastMessage = "",
            lastMessageSenderId = "",
            timestamp = System.currentTimeMillis(),
            participants = mapOf(
                currentUserId to true,
                otherUserId to true
            )
        )

        // Save chat metadata
        chatsRef.child(chatId).setValue(chat).await()

        // Add chat reference to both users
        userChatsRef.child(currentUserId).child(chatId).setValue(true).await()
        userChatsRef.child(otherUserId).child(chatId).setValue(true).await()

        return chatId
    }

    /**
     * Find existing chat between two users
     */
    private suspend fun findExistingChat(userId1: String, userId2: String): String? {
        return try {
            val snapshot = userChatsRef.child(userId1).get().await()
            
            for (child in snapshot.children) {
                val chatId = child.key ?: continue
                val chatSnapshot = chatsRef.child(chatId).get().await()
                val chat = chatSnapshot.getValue(Chat::class.java) ?: continue
                
                // Check if this is a 2-person chat with the other user
                if (chat.participants.size == 2 && 
                    chat.participants.containsKey(userId1) && 
                    chat.participants.containsKey(userId2)) {
                    return chatId
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get chat details by ID
     */
    suspend fun getChat(chatId: String): Chat? {
        return try {
            val snapshot = chatsRef.child(chatId).get().await()
            snapshot.getValue(Chat::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the display name for a chat (handles 1-on-1 vs group chats)
     */
    suspend fun getChatDisplayName(chatId: String): String {
        val chat = getChat(chatId) ?: return "Chat"
        val currentUserId = getCurrentUserId() ?: return chat.name
        
        // For 1-on-1 chats, show the other person's name
        if (chat.participants.size == 2) {
            val otherUserId = chat.participants.keys.find { it != currentUserId }
            if (otherUserId != null) {
                val otherUser = getUser(otherUserId)
                return otherUser?.displayName ?: chat.name
            }
        }
        
        return chat.name
    }

    // ==================== MESSAGE OPERATIONS ====================

    /**
     * Get messages for a chat (real-time updates)
     */
    fun getChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (child in snapshot.children) {
                    child.getValue(Message::class.java)?.let { message ->
                        messages.add(message)
                    }
                }
                // Sort by timestamp (oldest first for chat display)
                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { messagesRef.child(chatId).removeEventListener(listener) }
    }

    /**
     * Send a message to a chat
     */
    suspend fun sendMessage(chatId: String, text: String) {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val currentUserName = getCurrentUserName()
        
        val messageId = messagesRef.child(chatId).push().key 
            ?: throw Exception("Failed to create message")
        
        val timestamp = System.currentTimeMillis()
        
        val message = Message(
            id = messageId,
            senderId = currentUserId,
            senderName = currentUserName,
            text = text,
            timestamp = timestamp
        )

        // Save the message
        messagesRef.child(chatId).child(messageId).setValue(message).await()

        // Update chat's last message
        val updates = mapOf(
            "lastMessage" to text,
            "lastMessageSenderId" to currentUserId,
            "timestamp" to timestamp
        )
        chatsRef.child(chatId).updateChildren(updates).await()
    }

    // ==================== SIGN OUT ====================

    fun signOut() {
        auth.signOut()
    }
}
