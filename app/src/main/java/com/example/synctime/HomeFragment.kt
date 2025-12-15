package com.example.synctime

import android.Manifest
import android.app.AlertDialog
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
import android.widget.EditText
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
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.google.android.libraries.places.api.model.AutocompleteSessionToken

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var googleMap: GoogleMap? = null
    private lateinit var placesClient: PlacesClient
    private var lastLocation: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val DIRECTIONS_API_KEY by lazy { getString(R.string.google_maps_key) }

    // PredictionsAdapter is initialized AFTER placesClient is initialized
    private lateinit var predictionsAdapter: PredictionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Places API Client
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), DIRECTIONS_API_KEY)
        }
        placesClient = Places.createClient(requireContext())

        // Initialize the adapter after placesClient
        predictionsAdapter = PredictionsAdapter(
            onPredictionSelected = { prediction ->
                val address = prediction.getFullText(null).toString()
                fetchAndRenderRoute(address)
            },
            placesClient = placesClient
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView.getMapAsync { map ->
            googleMap = map
            checkLocationPermission() // Added method reference
        }

        view.findViewById<View>(R.id.btn_new_trip).setOnClickListener {
            showDestinationInputDialog()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission if not granted
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
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000L // Request location updates every 10 seconds
            fastestInterval = 5000L // Receive updates more frequently if available
            priority = Priority.PRIORITY_HIGH_ACCURACY // Use GPS for high accuracy
            smallestDisplacement = 50f // Only receive an update if the user moves 50 meters
        }

        // Define the location callback to handle responses
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    lastLocation = LatLng(location.latitude, location.longitude)
                    Log.d("HomeFragment", "Updated location: $lastLocation")
                }
            }
        }

        // Check permission and request location updates
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // Run the callback on the main thread
            )
        } else {
            Log.e("HomeFragment", "Location permission not granted")
        }
    }

    private fun showDestinationInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_autocomplete_trip, null)
        val addressInput = dialogView.findViewById<EditText>(R.id.input_address)
        val predictionsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.predictionsRecyclerView)

        // Bind RecyclerView to PredictionsAdapter
        predictionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        predictionsRecyclerView.adapter = predictionsAdapter

        // Create a session token for fetching predictions
        val sessionToken = AutocompleteSessionToken.newInstance()

        // Add listener for text input to fetch predictions
        addressInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    fetchAutocompletePredictions(query, sessionToken)
                } else {
                    predictionsAdapter.updatePredictions(emptyList()) // Clear predictions
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        builder.setView(dialogView)
            .setPositiveButton("Search") { _, _ ->
                val destination = addressInput.text.toString().trim()
                if (destination.isNotEmpty()) {
                    fetchAndRenderRoute(destination)
                } else {
                    Toast.makeText(requireContext(), "Please enter a destination", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()

        // Set dynamic width and height of the dialog
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog.window?.setLayout(width, height)
    }

    private fun fetchAutocompletePredictions(query: String, token: AutocompleteSessionToken) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                response.autocompletePredictions.forEach {
                    Log.d("HomeFragment", "Prediction: Primary: ${it.getPrimaryText(null)}, Secondary: ${it.getSecondaryText(null)}")
                }

                predictionsAdapter.updatePredictions(response.autocompletePredictions)
            }
            .addOnFailureListener { error ->
                Log.e("HomeFragment", "Failed to fetch predictions: ${error.message}")
            }
    }

    private fun fetchAndRenderRoute(destination: String) {
        val origin = lastLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Toast.makeText(requireContext(), "Location unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin&destination=${Uri.encode(destination)}&key=$DIRECTIONS_API_KEY"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch directions: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val routes = json.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No routes found.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val overviewPolyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                val points = decodePoly(overviewPolyline)

                requireActivity().runOnUiThread {
                    googleMap?.clear()
                    googleMap?.addPolyline(
                        PolylineOptions().addAll(points).color(Color.BLUE).width(10f)
                    )
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], 12f))
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
        private val placesClient: PlacesClient
    ) : RecyclerView.Adapter<PredictionsAdapter.PredictionViewHolder>() {

        private val predictions = mutableListOf<AutocompletePrediction>()

        inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val predictionText: TextView = itemView.findViewById(R.id.prediction_text) // Primary text
            val secondaryPredictionText: TextView = itemView.findViewById(R.id.secondary_prediction_text) // Secondary details
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_prediction, parent, false)
            return PredictionViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
            val prediction = predictions[position]

            // Get primary and secondary text from the `AutocompletePrediction` object
            val primaryText = prediction.getPrimaryText(null).toString() // Main suggestion (e.g., street address)
            val secondaryText = prediction.getSecondaryText(null)?.toString() ?: ""
            holder.secondaryPredictionText.text = secondaryText

            // Bind primary and secondary text to the respective TextViews
            holder.predictionText.text = primaryText
            holder.secondaryPredictionText.text = secondaryText

            Log.d("PredictionsAdapter", "Binding: Primary: $primaryText, Secondary: $secondaryText")

            // Handle click on the item
            holder.itemView.setOnClickListener {
                onPredictionSelected(prediction)
            }
        }

        override fun getItemCount(): Int = predictions.size

        fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
            predictions.clear()
            predictions.addAll(newPredictions) // Add new predictions
            notifyDataSetChanged() // Notify RecyclerView about the changes
            Log.d("PredictionsAdapter", "Adapter updated. Total predictions: ${predictions.size}")
        }
    }
}