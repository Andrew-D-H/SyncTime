package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChatFragment : Fragment() {

    private var friendUid: String? = null
    private var friendName: String? = null

    companion object {
        fun newInstance(uid: String, name: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString("friendUid", uid)
            args.putString("friendName", name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendUid = arguments?.getString("friendUid")
        friendName = arguments?.getString("friendName")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvName = view.findViewById<TextView>(R.id.tvChatFriendName)
        tvName.text = friendName ?: "Unknown"
    }
}
