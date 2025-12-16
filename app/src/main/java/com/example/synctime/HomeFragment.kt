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
import android.widget.ArrayAdapter
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import android.widget.EditText

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var googleMap: GoogleMap? = null
    private lateinit var placesClient: PlacesClient
    private var lastLocation: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val DIRECTIONS_API_KEY by lazy { getString(R.string.google_maps_key) }

    private lateinit var predictionsAdapter: PredictionsAdapter
    private var currentPlacesAdapter: SimplePlaceAdapter? = null
    
    // Recommendation system constants
    private val NEARBY_SEARCH_RADIUS = 5000 // meters
    private val MAX_PLACES_PER_CATEGORY = 8
    private val VICINITY_MAX_LENGTH = 40
    private val VICINITY_TRUNCATE_LENGTH = 37
    
    // Cache for UI elements
    private var categoryDropdownRef: MaterialAutoCompleteTextView? = null
    private var recommendedRecyclerViewRef: RecyclerView? = null

    data class PlaceItem(
        val name: String,
        val subtitle: String
    )

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
                    setText(address)
                    clearFocus()
                }

                // Start navigation (builds directions URL correctly)
                startNavigation(address)
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

        // Check if a destination was passed via arguments
        val passedDestination = arguments?.getString("trip_destination")
        if (!passedDestination.isNullOrEmpty()) {
            Log.d("HomeFragment", "Passed destination: $passedDestination")
            searchBar.setText(passedDestination)
            startNavigation(passedDestination)
        }

        // Map init
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView.getMapAsync { map ->
            googleMap = map
            checkLocationPermission()
        }

        // New Trip button
        view.findViewById<View>(R.id.btn_new_trip).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TripFragment())
                .addToBackStack(null)
                .commit()
        }

        // Open Trip button
        view.findViewById<View>(R.id.btn_open_trip).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OpenTripFragment())
                .addToBackStack(null)
                .commit()
        }

        // ─────────── Autocomplete Predictions RecyclerView ───────────
        val predictionsRecyclerView = view.findViewById<RecyclerView>(R.id.predictionsRecyclerView)

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

        // ─────────── Recommendation System Setup ───────────
        setupRecommendationSystem(view)
    }

    // 8 fake items per category with realistic subtitles
    private fun fakePlaceItems(category: String): List<PlaceItem> {
        val bases = listOf(
            "Oak Plaza",
            "Riverside Center",
            "Pine & Main",
            "Northside",
            "Lakeside",
            "Sunset Point",
            "City Center",
            "Grand Avenue"
        )

        val distances = listOf("0.4 mi", "0.7 mi", "1.1 mi", "1.6 mi", "2.0 mi", "2.4 mi", "3.1 mi", "3.8 mi")
        val hours = listOf(
            "Open until 9 PM",
            "Open until 10 PM",
            "Open until 11 PM",
            "Open until 8 PM",
            "Open 24/7",
            "Open until 7 PM",
            "Open until 6 PM",
            "Open until 12 AM"
        )
        val ratings = listOf("4.7", "4.6", "4.5", "4.4", "4.8", "4.3", "4.6", "4.5")
        val prices = listOf("$", "$$", "$$", "$$$", "$", "$$", "$$$", "$$")

        return bases.mapIndexed { i, base ->
            val title = when (category) {
                "Restaurant" -> "$base Bistro"
                "Gas Station" -> "$base Fuel"
                "Grocery Store" -> "$base Market"
                "Coffee Shop" -> "$base Coffee"
                "Pharmacy" -> "$base Pharmacy"
                "Superstore" -> "$base Superstore"
                else -> "$base $category"
            }

            val subtitle = when (category) {
                "Restaurant" ->
                    "★${ratings[i]} • ${prices[i]} • ${distances[i]} • ${hours[i]}"
                "Gas Station" ->
                    "${distances[i]} • ${if (i % 2 == 0) "Open 24/7" else hours[i]} • ${if (i % 3 == 0) "Car wash" else "Snacks"}"
                "Grocery Store" ->
                    "${distances[i]} • ${hours[i]} • ${if (i % 2 == 0) "Pickup available" else "Delivery"}"
                "Coffee Shop" ->
                    "★${ratings[i]} • ${distances[i]} • ${if (i % 2 == 0) "Drive-thru" else "Wi-Fi"}"
                "Pharmacy" ->
                    "${distances[i]} • ${hours[i]} • ${if (i % 2 == 0) "Vaccines" else "Prescriptions"}"
                "Superstore" ->
                    "${distances[i]} • ${hours[i]} • ${if (i % 2 == 0) "Pickup" else "Returns"}"
                else ->
                    "${distances[i]} • ${hours[i]}"
            }

            PlaceItem(name = title, subtitle = subtitle)
        }
    }

    private fun setupRecommendationSystem(view: View) {
        val categoryDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.categoryDropdown)
        val recommendedRecyclerView = view.findViewById<RecyclerView>(R.id.recommendedRecyclerView)
        
        // Cache view references for later use
        categoryDropdownRef = categoryDropdown
        recommendedRecyclerViewRef = recommendedRecyclerView

        // Set up RecyclerView
        recommendedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Define categories
        val categories = listOf(
            "Restaurant",
            "Gas Station",
            "Grocery Store",
            "Coffee Shop",
            "Pharmacy",
            "Superstore"
        )

        // Set up category dropdown
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)

        // Set default selection to "Restaurant"
        categoryDropdown.setText("Restaurant", false)
        
        // Fetch initial data for Restaurant
        fetchNearbyPlaces("Restaurant", recommendedRecyclerView)

        // Handle category selection
        categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            fetchNearbyPlaces(selectedCategory, recommendedRecyclerView)
        }
    }

    private fun fetchNearbyPlaces(category: String, recyclerView: RecyclerView) {
        val location = lastLocation
        if (location == null) {
            Log.w("HomeFragment", "Location not available yet. Using fake data for $category")
            // Use fake data as fallback
            val fakeItems = fakePlaceItems(category)
            currentPlacesAdapter = SimplePlaceAdapter(fakeItems)
            recyclerView.adapter = currentPlacesAdapter
            return
        }

        // Map category to Google Places API type
        val placeType = when (category) {
            "Restaurant" -> "restaurant"
            "Gas Station" -> "gas_station"
            "Grocery Store" -> "grocery_or_supermarket"
            "Coffee Shop" -> "cafe"
            "Pharmacy" -> "pharmacy"
            "Superstore" -> "supermarket"
            else -> "restaurant"
        }

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${location.latitude},${location.longitude}" +
                "&radius=$NEARBY_SEARCH_RADIUS" +
                "&type=$placeType" +
                "&key=$DIRECTIONS_API_KEY"

        Log.d("HomeFragment", "Fetching nearby places for $category")

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Log.e("HomeFragment", "Failed to fetch nearby places: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to fetch places. Using placeholder data.", Toast.LENGTH_SHORT).show()
                    // Fallback to fake data
                    val fakeItems = fakePlaceItems(category)
                    currentPlacesAdapter = SimplePlaceAdapter(fakeItems)
                    recyclerView.adapter = currentPlacesAdapter
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                
                try {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    
                    if (results == null || results.length() == 0) {
                        requireActivity().runOnUiThread {
                            Log.w("HomeFragment", "No places found for $category. Using fake data.")
                            val fakeItems = fakePlaceItems(category)
                            currentPlacesAdapter = SimplePlaceAdapter(fakeItems)
                            recyclerView.adapter = currentPlacesAdapter
                        }
                        return
                    }

                    val places = parseNearbyPlaces(results, category)
                    
                    requireActivity().runOnUiThread {
                        Log.d("HomeFragment", "Found ${places.size} places for $category")
                        currentPlacesAdapter = SimplePlaceAdapter(places)
                        recyclerView.adapter = currentPlacesAdapter
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        Log.e("HomeFragment", "Error parsing places: ${e.message}")
                        val fakeItems = fakePlaceItems(category)
                        currentPlacesAdapter = SimplePlaceAdapter(fakeItems)
                        recyclerView.adapter = currentPlacesAdapter
                    }
                }
            }
        })
    }

    private fun parseNearbyPlaces(results: JSONArray, category: String): List<PlaceItem> {
        val places = mutableListOf<PlaceItem>()
        
        for (i in 0 until minOf(results.length(), MAX_PLACES_PER_CATEGORY)) {
            try {
                val place = results.getJSONObject(i)
                val name = place.optString("name", "Unknown Place")
                val vicinity = place.optString("vicinity", "")
                
                // Get additional details
                val rating = place.optDouble("rating", 0.0)
                val isOpen = place.optJSONObject("opening_hours")?.optBoolean("open_now", false) ?: false
                val priceLevel = place.optInt("price_level", 0)
                
                // Build subtitle based on category
                val subtitle = buildPlaceSubtitle(category, rating, isOpen, priceLevel, vicinity)
                
                places.add(PlaceItem(name = name, subtitle = subtitle))
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error parsing place at index $i: ${e.message}")
            }
        }
        
        return places
    }

    private fun buildPlaceSubtitle(
        category: String,
        rating: Double,
        isOpen: Boolean,
        priceLevel: Int,
        vicinity: String
    ): String {
        val parts = mutableListOf<String>()
        
        // Add rating if available
        if (rating > 0) {
            parts.add("★${String.format("%.1f", rating)}")
        }
        
        // Add price level for restaurants
        if (category == "Restaurant" && priceLevel > 0) {
            parts.add("$".repeat(priceLevel))
        }
        
        // Add open status
        parts.add(if (isOpen) "Open now" else "Closed")
        
        // Add vicinity/address (truncate if too long)
        if (vicinity.isNotEmpty()) {
            val truncatedVicinity = if (vicinity.length > VICINITY_MAX_LENGTH) {
                vicinity.substring(0, VICINITY_TRUNCATE_LENGTH) + "..."
            } else {
                vicinity
            }
            parts.add(truncatedVicinity)
        }
        
        return parts.joinToString(" • ")
    }

    private class SimplePlaceAdapter(
        private val items: List<PlaceItem>
    ) : RecyclerView.Adapter<SimplePlaceAdapter.PlaceVH>() {

        class PlaceVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.place_title)
            val address: TextView = itemView.findViewById(R.id.place_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
            return PlaceVH(v)
        }

        override fun onBindViewHolder(holder: PlaceVH, position: Int) {
            val item = items[position]
            holder.title.text = item.name
            holder.address.text = item.subtitle
        }

        override fun getItemCount(): Int = items.size
    }

    private fun startNavigation(destinationAddress: String) {
        val origin = lastLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Toast.makeText(requireContext(), "Current location not available. Please wait for GPS fix.", Toast.LENGTH_SHORT).show()
            Log.e("HomeFragment", "Cannot start navigation: lastLocation is null.")
            return
        }

        Log.d("HomeFragment", "Starting navigation from: $origin to: $destinationAddress")

        val directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$origin" +
                "&destination=${Uri.encode(destinationAddress)}" +
                "&key=${resources.getString(R.string.google_maps_key)}"

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
            interval = 5000L
            fastestInterval = 2000L
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 0f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val wasNull = lastLocation == null
                    lastLocation = LatLng(location.latitude, location.longitude)
                    Log.d("HomeFragment", "Real-time location updated: ${location.latitude}, ${location.longitude}")
                    
                    // If this is the first location update, refresh recommendations with real data
                    if (wasNull && categoryDropdownRef != null && recommendedRecyclerViewRef != null) {
                        val currentCategory = categoryDropdownRef?.text.toString()
                        if (currentCategory.isNotEmpty()) {
                            fetchNearbyPlaces(currentCategory, recommendedRecyclerViewRef!!)
                        }
                    }
                    
                    onLocationUpdated()
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e("HomeFragment", "Location permission not granted. Cannot start location updates.")
        }
    }

    private fun onLocationUpdated() {
        val origin = lastLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Log.e("HomeFragment", "Cannot refresh route: Current location (origin) not available.")
            return
        }

        val destination = arguments?.getString("trip_destination") ?: return

        Log.d("HomeFragment", "Refreshing route from: $origin to: $destination")

        val directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$origin" +
                "&destination=${Uri.encode(destination)}" +
                "&key=${resources.getString(R.string.google_maps_key)}"

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
                    view?.findViewById<RecyclerView>(R.id.predictionsRecyclerView)?.visibility = View.VISIBLE
                } else {
                    predictionsAdapter.updatePredictions(emptyList())
                    view?.findViewById<RecyclerView>(R.id.predictionsRecyclerView)?.visibility = View.GONE
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

                val routePoints = decodePoly(overviewPolyline)

                requireActivity().runOnUiThread {
                    // Clear previous map route
                    googleMap?.clear()

                    googleMap?.addPolyline(
                        PolylineOptions()
                            .addAll(routePoints)
                            .color(Color.BLUE)
                            .width(10f)
                    )

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

            val primaryText = prediction.getPrimaryText(null).toString()
            val secondaryText = prediction.getSecondaryText(null)?.toString() ?: ""

            holder.predictionText.text = primaryText
            holder.secondaryPredictionText.text = secondaryText

            holder.itemView.setOnClickListener {
                onPredictionSelected(prediction)
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
