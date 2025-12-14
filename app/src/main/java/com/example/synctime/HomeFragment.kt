package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import android.app.AlertDialog
import android.widget.Button
import android.widget.EditText

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─────────── RecyclerView Setup ───────────
        val recyclerView = view.findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val fakePlaces = listOf(
            "7747 State Street" to "Green Cove Springs, FL 32043",
            "123 Main Street" to "Orlando, FL 32801",
            "456 Oak Avenue" to "Jacksonville, FL 32202",
            "789 Pine Lane" to "Miami, FL 33101",
            "101 Maple Drive" to "Tampa, FL 33602"
        )

        recyclerView.adapter = PlacesAdapter(fakePlaces)

        // ─────────── Floating Button Setup ───────────
        val btnNewTrip = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_new_trip)
        val btnChat = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_chat)

        // ─────────── New Trip Button Functionality ───────────
        btnNewTrip.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).duration = 100
                }

            // Inflate custom dialog layout
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_trip, null)
            val tripInput = dialogView.findViewById<EditText>(R.id.trip_name_input)
            val createButton = dialogView.findViewById<Button>(R.id.btn_create_trip)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            createButton.setOnClickListener {
                val tripName = tripInput.text.toString().trim()
                if (tripName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a trip name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Trip '$tripName' created!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            dialog.show()
        }

        // ─────────── Chat Button Functionality ───────────
        btnChat.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).duration = 100
                }

            // 🔹 Navigate to ChatListFragment
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, ChatListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // 🔹 RecyclerView Adapter
    inner class PlacesAdapter(private val places: List<Pair<String, String>>) :
        RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

        inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.place_title)
            val address: TextView = itemView.findViewById(R.id.place_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
            return PlaceViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
            val (title, address) = places[position]
            holder.title.text = title
            holder.address.text = address
        }

        override fun getItemCount() = places.size
    }
}
