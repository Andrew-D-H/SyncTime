package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class NewChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnCreate: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_new_chat, container, false)

        recycler = view.findViewById(R.id.recyclerUsers)
        btnCreate = view.findViewById(R.id.btnCreate)

        btnCreate.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}
