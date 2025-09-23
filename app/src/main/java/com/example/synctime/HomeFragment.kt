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

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val fakePlaces = listOf(
            "7747 State Street" to "Green Cove Springs, FL 32043",
            "123 Main Street" to "Orlando, FL 32801",
            "456 Oak Avenue" to "Jacksonville, FL 32202",
            "789 Pine Lane" to "Miami, FL 33101",
            "101 Maple Drive" to "Tampa, FL 33602",
            "101 Maple Drive" to "Tampa, FL 33602"
        )

        recyclerView.adapter = PlacesAdapter(fakePlaces)
        Toast.makeText(requireContext(), "Adapter set with ${fakePlaces.size} places", Toast.LENGTH_SHORT).show()
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
