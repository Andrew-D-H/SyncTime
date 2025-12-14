package com.example.synctime

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var googleMap: GoogleMap? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Store last location for directions
    private var lastLocation: LatLng? = null

    private val DIRECTIONS_API_KEY = "@string/google_maps_key"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─────────── RecyclerView Setup ───────────
        val recyclerView = view.findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val fakePlaces = listOf(
            "7747 State Street" to "Green Cove Springs, FL 32043",
            "123 Main Street" to "Orlando, FL 32801",
            "456 Oak Avenue" to "Jacksonville, FL 32202",
            "789 Pine Lane" to "Miami, FL 33101",
            "101 Maple Drive" to "Tampa, FL 33602"
        )

        recyclerView.adapter = PlacesAdapter(fakePlaces)

        // ─────────── Floating Button Setup ───────────
        val btnNewTrip = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_new_trip)
        val btnChat = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_chat)

        // ─────────── New Trip Button Functionality ───────────
        btnNewTrip.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).duration = 100
                }

            // Inflate custom dialog layout
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_trip, null)
            val tripInput = dialogView.findViewById<EditText>(R.id.trip_name_input)
            val createButton = dialogView.findViewById<Button>(R.id.btn_create_trip)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            createButton.setOnClickListener {
                val tripName = tripInput.text.toString().trim()
                if (tripName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a trip name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Trip '$tripName' created!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            dialog.show()
        }

        // ─────────── Chat Button Functionality ───────────
        btnChat.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).duration = 100
                }

            AlertDialog.Builder(requireContext())
                .setTitle("Chat")
                .setMessage("Chat feature coming soon!")
                .setPositiveButton("OK", null)
                .show()
        }
        // Google Maps setup
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView.getMapAsync { map ->
            googleMap = map
            checkLocationPermission()
        }

        // New Trip button for route input
        view.findViewById<View>(R.id.btn_new_trip)?.setOnClickListener {
            showDestinationInputDialog()
        }
    }

    // Permissions check and request
    private fun checkLocationPermission() {
        val context = requireContext()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
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

    // Permission result callback
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            val defaultLocation = LatLng(29.8507, -81.6868) // Example: Green Cove Springs
            googleMap?.addMarker(MarkerOptions().position(defaultLocation).title("Green Cove Springs"))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
        }
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    lastLocation = latLng
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // Dialog to input destination
    private fun showDestinationInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Destination")

        val input = EditText(requireContext())
        input.hint = "Destination address"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val destination = input.text.toString()
            if (destination.isNotBlank()) {
                fetchAndShowDirections(destination)
            } else {
                Toast.makeText(requireContext(), "Please enter a destination", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // Fetch directions from API and show on map
    private fun fetchAndShowDirections(destination: String) {
        val origin = lastLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Toast.makeText(requireContext(), "Current location not available.", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=${Uri.encode(destination)}&key=$DIRECTIONS_API_KEY"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch directions", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No route found", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val overviewPolyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                val points = decodePoly(overviewPolyline)
                requireActivity().runOnUiThread {
                    googleMap?.clear()
                    googleMap?.addPolyline(PolylineOptions().addAll(points).color(Color.BLUE).width(10f))
                    if (points.isNotEmpty()) googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], 12f))
                }
            }
        })
    }

    // Polyline decoder for Google Directions API response
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
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
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        mapView.onLowMemory()
        super.onLowMemory()
    }

    // RecyclerView Adapter
    inner class PlacesAdapter(private val places: List<Pair<String, String>>) :
        RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

        inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.place_title)
            val address: TextView = itemView.findViewById(R.id.place_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
            return PlaceViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
            val (title, address) = places[position]
            holder.title.text = title
            holder.address.text = address
        }

        override fun getItemCount() = places.size
    }
}