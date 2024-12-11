package com.example.lionchat.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import androidx.lifecycle.ViewModel
import com.example.lionchat.R
import com.example.lionchat.model.Message
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class  ChatViewModel @Inject constructor(@ApplicationContext val context: Context) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()
    private val db = Firebase.database

    fun sendMessage(channelID: String, messageText: String?, image: String? = null) {
        if (messageText.isNullOrBlank() && image.isNullOrBlank()) { return }

        val message = Message(
            db.reference.push().key ?: UUID.randomUUID().toString(),
            Firebase.auth.currentUser?.uid ?: "",
            messageText,
            System.currentTimeMillis(),
            Firebase.auth.currentUser?.displayName ?: "",
            null,
            image
        )
        db.reference.child("messages").child(channelID).push().setValue(message)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    postNotificationToUsers(channelID, message.senderName, messageText ?: "")
                }
            }
    }

    fun sendImageMessage(uri: Uri, channelID: String) {
        val imageRef = Firebase.storage.reference.child("images/${UUID.randomUUID()}")
        imageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                val currentUser = Firebase.auth.currentUser
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    sendMessage(channelID, null, downloadUri.toString())
                }
            }
    }

    fun listenForMessages(channelID: String) {
        db.getReference("messages").child(channelID).orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Message>()
                    snapshot.children.forEach { data ->
                        val message = data.getValue(Message::class.java)
                        message?.let { list.add(it) }
                    }
                    _messages.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }
            })
        subscribeForNotification(channelID)
    }

    private fun subscribeForNotification(channelID: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("group_$channelID")
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("ChatViewModel", "Subscribed to topic: group_$channelID")
                } else {
                    Log.d("ChatViewModel", "Failed to subscribe to topic: group_$channelID")
                    // Handle failure
                }
            }
    }

    private fun postNotificationToUsers(channelID: String, senderName: String, messageContent: String
    ) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/lionchat-fca3e/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("topic", "group_$channelID")
                put("notification", JSONObject().apply {
                    put("title", "New message in $channelID")
                    put("body", "$senderName: $messageContent")
                })
            })
        }
        val requestBody = jsonBody.toString()

        val request = object : StringRequest(Method.POST, fcmUrl, Response.Listener {
            Log.d("ChatViewModel", "Notification sent successfully")
        }, Response.ErrorListener {
            Log.e("ChatViewModel", "Failed to send notification")
        }) {
            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${getAccessToken()}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    private fun getAccessToken(): String {
        val inputStream = context.resources.openRawResource(R.raw.lionchat_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        return googleCreds.refreshAccessToken().tokenValue
    }
}