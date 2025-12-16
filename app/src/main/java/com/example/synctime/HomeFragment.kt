package com.example.synctime

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.widget.EditText
import android.widget.RadioButton

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var googleMap: GoogleMap? = null
    private lateinit var placesClient: PlacesClient
    private var lastLocation: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val DIRECTIONS_API_KEY by lazy { getString(R.string.google_maps_key) }
    private var travelModeChar: Char = 'd'
    private lateinit var predictionsAdapter: PredictionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Places API Client
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), DIRECTIONS_API_KEY)
        }
        placesClient = Places.createClient(requireContext())

        // Initialize PredictionsAdapter
        predictionsAdapter = PredictionsAdapter(
            onPredictionSelected = { prediction ->
                val address = prediction.getFullText(null).toString()

                // Update the search bar
                view?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.search_bar)?.apply {
                    setText(address) // Set the address in the search bar
                    clearFocus() // Remove focus from the search bar
                }

                // Render the route for the selected address
                fetchAndRenderRoute(address)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference the search bar (destination input field)
        val searchBar = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.search_bar)

        if (searchBar == null) {
            Log.e("HomeFragment", "search_bar is not found in the layout. Ensure it's defined in fragment_home.xml.")
            return
        }

        //Set travel mode based on selected button
        view.findViewById<RadioButton>(R.id.radioButtonDrive).setOnClickListener { buttonView, ->
            Toast.makeText(requireContext(), "Travel Mode set to Driving", Toast.LENGTH_SHORT).show()
            travelModeChar = 'd'
        }

        view.findViewById<RadioButton>(R.id.radioButtonTransit).setOnClickListener{ buttonView, ->
            Toast.makeText(requireContext(), "Travel Mode set to Public Transit", Toast.LENGTH_SHORT).show()
            travelModeChar = 't'
        }
        view.findViewById<RadioButton>(R.id.radioButtonWalk).setOnClickListener { buttonView ->
            Toast.makeText(requireContext(), "Travel Mode set to Walking", Toast.LENGTH_SHORT).show()
            travelModeChar = 'w'
        }
        // Check if a destination was passed via arguments
        val passedDestination = arguments?.getString("trip_destination")
        if (!passedDestination.isNullOrEmpty()) {
            Log.d("HomeFragment", "Passed destination: $passedDestination")

            // Populate the input field for visual feedback
            searchBar.setText(passedDestination)

            // Start navigation immediately
            startNavigation(passedDestination)
        } else {
            Log.e("HomeFragment", "No trip destination passed from OpenTripFragment.")
            Toast.makeText(requireContext(), "No destination passed from trip selection.", Toast.LENGTH_SHORT).show()
        }

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView.getMapAsync { map ->
            googleMap = map
            checkLocationPermission()
        }

        // Navigate to TripFragment on "New com.example.synctime.Trip" button click
        view.findViewById<View>(R.id.btn_new_trip).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TripFragment())
                .addToBackStack(null) // Allows the user to navigate back
                .commit()
        }

        // Open com.example.synctime.Trip Button
        view.findViewById<View>(R.id.btn_open_trip).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OpenTripFragment()) // Open a fragment that lists available trips
                .addToBackStack(null)
                .commit()
        }


        val predictionsRecyclerView = view.findViewById<RecyclerView>(R.id.placesRecyclerView)

        predictionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        predictionsRecyclerView.adapter = predictionsAdapter

        // Listen for input in the search bar
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    fetchAutocompletePredictions(query)
                } else {
                    predictionsAdapter.updatePredictions(emptyList())
                    predictionsRecyclerView.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun startNavigation(destinationAddress: String) {
        val origin = lastLocation?.let {
            "${it.latitude},${it.longitude}"
        } ?: run {
            // Wait for the location to be available before starting navigation
            Toast.makeText(requireContext(), "Current location not available. Please wait for GPS fix.", Toast.LENGTH_SHORT).show()
            Log.e("HomeFragment", "Cannot start navigation: lastLocation is null.")
            return
        }

        // Log navigation start
        Log.d("HomeFragment", "Starting navigation from: $origin to: $destinationAddress")

        // Build the Google Directions API URL
        val directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$origin" +
                "&destination=${Uri.encode(destinationAddress)}" +
                "&" + travelModeChar +
                "&key=${resources.getString(R.string.google_maps_key)}"

        // Start fetching and rendering the route
        fetchAndRenderRoute(directionsApiUrl)
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableUserLocation()
        }
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000L  // Request location updates every 5 seconds
            fastestInterval = 2000L  // Receive updates as fast as every 2 seconds if other apps are using location
            priority = Priority.PRIORITY_HIGH_ACCURACY  // Use GPS when available
            smallestDisplacement = 0f  // Track even small movements
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    lastLocation = LatLng(location.latitude, location.longitude)
                    Log.d("HomeFragment", "Real-time location updated: ${location.latitude}, ${location.longitude}")

                    // Trigger route update on location change
                    onLocationUpdated()
                }
            }
        }

        // Ensure permissions are granted before requesting location updates
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e("HomeFragment", "Location permission not granted. Cannot start location updates.")
        }
    }


    private fun onLocationUpdated() {
        // Ensure the origin (current location) is available
        val origin = lastLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Log.e("HomeFragment", "Cannot refresh route: Current location (origin) not available.")
            return
        }

        // Retrieve the destination (trip_destination)
        val destination = arguments?.getString("trip_destination") ?: run {
            Log.e("HomeFragment", "Cannot refresh route: Destination is not specified.")
            return
        }

        Log.d("HomeFragment", "Refreshing route from: $origin to: $destination")

        // Build the Directions API URL
        val directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$origin" +
                "&destination=${Uri.encode(destination)}" +
                "&key=${resources.getString(R.string.google_maps_key)}"

        // Fetch and render the updated route
        fetchAndRenderRoute(directionsApiUrl)
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
                    view?.findViewById<RecyclerView>(R.id.placesRecyclerView)?.visibility = View.VISIBLE
                } else {
                    predictionsAdapter.updatePredictions(emptyList())
                    view?.findViewById<RecyclerView>(R.id.placesRecyclerView)?.visibility = View.GONE
                }
            }
            .addOnFailureListener { error ->
                Log.e("HomeFragment", "Error fetching predictions: ${error.message}")
            }
    }

    private fun fetchAndRenderRoute(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Log.e("HomeFragment", "Failed to fetch directions: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to fetch directions. Check your network.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val routes = json.optJSONArray("routes")

                if (routes == null || routes.length() == 0) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No routes found for this destination.", Toast.LENGTH_SHORT).show()
                        Log.e("HomeFragment", "No routes found in response: $json")
                    }
                    return
                }

                val overviewPolyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                // Decode the polyline points
                val routePoints = decodePoly(overviewPolyline)

                requireActivity().runOnUiThread {
                    // Clear previous map route
                    googleMap?.clear()

                    // Draw the new route on the map
                    googleMap?.addPolyline(
                        PolylineOptions()
                            .addAll(routePoints)
                            .color(Color.BLUE)
                            .width(10f)
                    )

                    // Move the camera to the updated location/route
                    if (routePoints.isNotEmpty()) {
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints[0], 15f))
                    }
                }
            }
        })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }

    private inner class PredictionsAdapter(
        private val onPredictionSelected: (AutocompletePrediction) -> Unit,
    ) : RecyclerView.Adapter<PredictionsAdapter.PredictionViewHolder>() {

        private val predictions = mutableListOf<AutocompletePrediction>()

        inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val predictionText: TextView = itemView.findViewById(R.id.prediction_text)
            val secondaryPredictionText: TextView = itemView.findViewById(R.id.secondary_prediction_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
            return PredictionViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
            val prediction = predictions[position]

            // Bind primary and secondary text to TextViews
            val primaryText = prediction.getPrimaryText(null).toString()
            val secondaryText = prediction.getSecondaryText(null)?.toString() ?: ""

            holder.predictionText.text = primaryText
            holder.secondaryPredictionText.text = secondaryText

            // Handle item clicks
            holder.itemView.setOnClickListener {
                // Update the search bar with the selected address
                onPredictionSelected(prediction)

                // Hide the predictions list from the RecyclerView
                (holder.itemView.parent as? RecyclerView)?.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = predictions.size

        fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
            predictions.clear()
            predictions.addAll(newPredictions)
            notifyDataSetChanged()
        }
    }
}