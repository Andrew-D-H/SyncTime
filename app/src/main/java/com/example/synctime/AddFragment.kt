package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class AddFragment : Fragment(R.layout.fragment_add) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripNameInput = view.findViewById<EditText>(R.id.trip_name_input)
        val destinationInput = view.findViewById<EditText>(R.id.trip_destination_input)
        val dateInput = view.findViewById<EditText>(R.id.trip_date_input)
        val timeInput = view.findViewById<EditText>(R.id.trip_time_input)
        val createButton = view.findViewById<Button>(R.id.btn_create_trip)

        createButton.setOnClickListener {
            val tripName = tripNameInput.text.toString().trim()
            val destination = destinationInput.text.toString().trim()
            val date = dateInput.text.toString().trim()
            val time = timeInput.text.toString().trim()

            if (tripName.isEmpty() || destination.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Trip Created:\n$tripName → $destination\n$date at $time",
                    Toast.LENGTH_LONG
                ).show()

                // Later: Save this data to Firebase or local database
                tripNameInput.text.clear()
                destinationInput.text.clear()
                dateInput.text.clear()
                timeInput.text.clear()
            }
        }
    }
}
