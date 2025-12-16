package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction

class PredictionsAdapter(
    private val onPredictionClick: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PredictionsAdapter.PredictionViewHolder>() {

    private val predictions = mutableListOf<AutocompletePrediction>()

    inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val predictionText: TextView = itemView.findViewById(R.id.prediction_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return PredictionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        val prediction = predictions[position]
        holder.predictionText.text = prediction.getFullText(null).toString()

        holder.itemView.setOnClickListener {
            onPredictionClick(prediction)
        }
    }

    override fun getItemCount() = predictions.size

    fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
        predictions.clear()
        predictions.addAll(newPredictions)
        notifyDataSetChanged()
    }
}