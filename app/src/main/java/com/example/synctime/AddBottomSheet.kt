package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripName = view.findViewById<EditText>(R.id.trip_name_input)
        val destination = view.findViewById<EditText>(R.id.trip_destination_input)
        val date = view.findViewById<EditText>(R.id.trip_date_input)
        val time = view.findViewById<EditText>(R.id.trip_time_input)
        val createButton = view.findViewById<Button>(R.id.btn_create_trip)

        createButton.setOnClickListener {
            val name = tripName.text.toString()
            val dest = destination.text.toString()
            val tripDate = date.text.toString()
            val tripTime = time.text.toString()

            if (name.isEmpty() || dest.isEmpty() || tripDate.isEmpty() || tripTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Trip Created: $name → $dest ($tripDate at $tripTime)",
                    Toast.LENGTH_LONG
                ).show()
                dismiss() // Close the bottom sheet after success
            }
        }
    }
}
