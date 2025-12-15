package com.example.synctime

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

class TripFragment : Fragment(R.layout.trip) {

    private lateinit var placesClient: PlacesClient
    private lateinit var predictionsAdapter: PredictionsAdapter
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var predictionsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView

    private val selectedFriendIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(requireContext())

        predictionsAdapter = PredictionsAdapter { prediction ->
            val address = prediction.getFullText(null).toString()
            view?.findViewById<TextInputEditText>(R.id.trip_destination_input)?.apply {
                setText(address)
                clearFocus()
            }
            predictionsRecyclerView.visibility = View.GONE
        }

        friendsAdapter = FriendsAdapter(
            context = requireContext(),
            onFriendSelected = { friendUid, isSelected ->
                if (isSelected) {
                    selectedFriendIds.add(friendUid)
                } else {
                    selectedFriendIds.remove(friendUid)
                }
                Log.d("TripFragment", "Selected friends: $selectedFriendIds")
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripNameInput = view.findViewById<TextInputEditText>(R.id.trip_name_input)
        val destinationInput = view.findViewById<TextInputEditText>(R.id.trip_destination_input)
        predictionsRecyclerView = view.findViewById(R.id.predictionsRecyclerView)
        friendsRecyclerView = view.findViewById(R.id.friends_list_recycler_view)

        predictionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        predictionsRecyclerView.adapter = predictionsAdapter

        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.adapter = friendsAdapter

        loadFriendsFromFirebase()

        destinationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    fetchAutocompletePredictions(query)
                } else {
                    predictionsRecyclerView.visibility = View.GONE
                    predictionsAdapter.updatePredictions(emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<Button>(R.id.btn_create_trip).setOnClickListener {
            val tripName = tripNameInput.text.toString()
            val destination = destinationInput.text.toString()

            if (tripName.isBlank() || destination.isBlank()) {
                Toast.makeText(requireContext(), "Please complete all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveTripToFirebase(tripName, destination, selectedFriendIds)
        }
    }

    private fun fetchAutocompletePredictions(query: String) {
        val sessionToken = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isNotEmpty()) {
                    predictionsAdapter.updatePredictions(predictions)
                    predictionsRecyclerView.visibility = View.VISIBLE
                } else {
                    predictionsRecyclerView.visibility = View.GONE
                    predictionsAdapter.updatePredictions(emptyList())
                }
            }
            .addOnFailureListener { error ->
                Log.e("TripFragment", "Error fetching predictions: ${error.message}")
                Toast.makeText(requireContext(), "Error fetching suggestions.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFriendsFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val friendsRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/friends")

        friendsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendsList = mutableListOf<Pair<String, Friend>>()
                val usersRef = FirebaseDatabase.getInstance().getReference("users")

                snapshot.children.forEach { child ->
                    val friendUid = child.key ?: return@forEach
                    usersRef.child(friendUid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(friendSnapshot: DataSnapshot) {
                            val friend = friendSnapshot.getValue(Friend::class.java) ?: return
                            friendsList.add(Pair(friendUid, friend))

                            if (friendsList.size.toLong() == snapshot.childrenCount) {
                                friendsAdapter.updateData(friendsList)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("TripFragment", "Error fetching friend details: ${error.message}")
                            Toast.makeText(requireContext(), "Failed to fetch friends.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TripFragment", "Error fetching friends: ${error.message}")
                Toast.makeText(requireContext(), "Failed to fetch friends.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveTripToFirebase(tripName: String, destination: String, invitedFriends: Set<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You need to be logged in to create a trip!", Toast.LENGTH_SHORT).show()
            return
        }

        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        // Generate unique trip ID using UUID
        val tripId = UUID.randomUUID().toString()

        // Create participants map
        val participants = invitedFriends.associateWith { true }.toMutableMap()
        // Add creator to the participants map
        participants[currentUser.uid] = true

        // Build trip data structure
        val tripData = mapOf(
            "creator" to currentUser.uid,
            "name" to tripName,
            "destination" to destination,
            "participants" to participants // Map structure for participants
        )

        // Save trip data to Firebase
        tripsRef.child(tripId).setValue(tripData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Trip '$tripName' created successfully!", Toast.LENGTH_SHORT).show()
                Log.d("TripFragment", "Trip added to Firebase: $tripData")
            }
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), "Failed to create trip: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("TripFragment", "Failed to save trip: ${error.message}")
            }
    }
}