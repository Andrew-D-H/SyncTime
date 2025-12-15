package com.example.synctime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OpenTripFragment : Fragment(R.layout.fragment_open_trip) {

    private lateinit var tripsAdapter: TripsAdapter
    private lateinit var tripsList: RecyclerView
    private val trips = mutableListOf<Trip>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripsList = view.findViewById(R.id.trips_list_recycler_view)
        tripsAdapter = TripsAdapter(trips) { selectedTrip ->
            onTripSelected(selectedTrip)
        }
        tripsList.layoutManager = LinearLayoutManager(requireContext())
        tripsList.adapter = tripsAdapter

        fetchUserTrips()
    }

    private fun fetchUserTrips() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No user logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("trips")
        val userId = currentUser.uid

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trips.clear()

                if (snapshot.exists()) {
                    for (tripSnapshot in snapshot.children) {
                        val trip = tripSnapshot.getValue(Trip::class.java)
                        if (trip != null && trip.participants.containsKey(userId)) {
                            trips.add(trip)
                            Log.d("OpenTripFragment", "Loaded trip: ${trip.name}, Destination: ${trip.destination}")
                        }
                    }
                    tripsAdapter.notifyDataSetChanged()

                    if (trips.isEmpty()) {
                        Toast.makeText(requireContext(), "No trips found!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No trips found!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch trips.", Toast.LENGTH_SHORT).show()
                Log.e("OpenTripFragment", "Error fetching trips: ${error.message}")
            }
        })
    }

    private fun onTripSelected(selectedTrip: Trip) {
        Log.d("OpenTripFragment", "Selected trip: ${selectedTrip.name}, Destination: ${selectedTrip.destination}")

        if (!selectedTrip.destination.isBlank()) {
            val bundle = Bundle().apply {
                putString("trip_destination", selectedTrip.destination)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment().apply { arguments = bundle })
                .addToBackStack(null)
                .commit()
        } else {
            Toast.makeText(requireContext(), "Trip destination is not available.", Toast.LENGTH_SHORT).show()
        }
    }
}