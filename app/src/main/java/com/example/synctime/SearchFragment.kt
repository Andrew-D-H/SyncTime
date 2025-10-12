package com.example.synctime

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchFragment : Fragment(R.layout.fragment_search) {

    private val allPlaces = listOf(
        "Starbucks" to "123 Main St, Long Beach",
        "Target" to "501 Pine Ave, Long Beach",
        "Trader Joe’s" to "601 Ocean Blvd, Long Beach",
        "In-N-Out Burger" to "1001 Atlantic Ave, Long Beach",
        "CSULB Library" to "1250 Bellflower Blvd, Long Beach"
    )

    private var filteredPlaces = allPlaces.toMutableList()
    private lateinit var adapter: SearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.search_input)
        val recycler = view.findViewById<RecyclerView>(R.id.search_results_recycler)

        adapter = SearchAdapter(filteredPlaces) { place ->
            Toast.makeText(requireContext(), "Selected: ${place.first}", Toast.LENGTH_SHORT).show()
            // Later: return selected place to HomeFragment
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Filter as user types
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filteredPlaces.clear()
                filteredPlaces.addAll(allPlaces.filter { it.first.lowercase().contains(query) })
                adapter.notifyDataSetChanged()
            }
        })
    }

    // 🔹 RecyclerView Adapter
    inner class SearchAdapter(
        private val places: List<Pair<String, String>>,
        private val onClick: (Pair<String, String>) -> Unit
    ) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

        inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.place_title)
            val address: TextView = itemView.findViewById(R.id.place_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
            return SearchViewHolder(view)
        }

        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            val place = places[position]
            holder.title.text = place.first
            holder.address.text = place.second
            holder.itemView.setOnClickListener { onClick(place) }
        }

        override fun getItemCount() = places.size
    }
}
