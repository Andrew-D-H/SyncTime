package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

// Model class for a Search Result
data class SearchResult(
    val uid: String,
    val displayName: String,
    val additionalInfo: String? = null
)

class SearchAdapter(
    private val onAddClick: (SearchResult) -> Unit,
    private val onCancelClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    // Immutable list of search results (contents can be updated, but the reference cannot be reassigned)
    private val results: MutableList<SearchResult> = mutableListOf()

    // Function to update the list of search results
    fun updateResults(newResults: List<SearchResult>) {
        results.clear() // Clear the previous results
        results.addAll(newResults) // Add new results
        notifyDataSetChanged() // Notify RecyclerView about data changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_user, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val searchResult = results[position]

        // Bind the data to the ViewHolder
        holder.displayName.text = searchResult.displayName
        holder.additionalInfo.text = searchResult.additionalInfo ?: "No additional info available"

        // Handle the "Add" button click
        holder.addButton.setOnClickListener {
            onAddClick(searchResult)
        }

        // Handle the "Cancel" button click
        holder.cancelButton.setOnClickListener {
            onCancelClick(searchResult)
        }
    }

    override fun getItemCount(): Int = results.size

    // Custom ViewHolder to handle item views
    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val displayName: TextView = itemView.findViewById(R.id.searchDisplayName)
        val additionalInfo: TextView = itemView.findViewById(R.id.searchAdditionalInfo)
        val addButton: View = itemView.findViewById(R.id.btnAdd)
        val cancelButton: View = itemView.findViewById(R.id.btnCancel)
    }
}