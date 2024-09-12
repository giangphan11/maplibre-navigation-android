package com.mapbox.services.android.navigation.testapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.testapp.databinding.ActivityNavigationUiBinding
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationRoute
import com.mapbox.services.android.navigation.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.models.RouteOptions
import com.mapbox.services.android.navigation.v5.navigation.*
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import kotlin.time.Duration

class NavigationUIActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener {
    private lateinit var mapboxMap: MapboxMap

    // Navigation related variables
    private var route: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var locationComponent: LocationComponent? = null

    private lateinit var binding: ActivityNavigationUiBinding

    private var simulateRoute = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        binding = ActivityNavigationUiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapView.apply {
            onCreate(savedInstanceState)
            getMapAsync(this@NavigationUIActivity)
        }

        binding.startRouteButton.setOnClickListener {
            route?.let { route ->
                val userLocation = mapboxMap.locationComponent.lastKnownLocation ?: return@let

                val options = NavigationLauncherOptions.builder()
                    .directionsRoute(route)
                    .shouldSimulateRoute(simulateRoute)
                    .initialMapCameraPosition(CameraPosition.Builder().target(LatLng(userLocation.latitude, userLocation.longitude)).build())
                    .lightThemeResId(R.style.TestNavigationViewLight)
                    .darkThemeResId(R.style.TestNavigationViewDark)
                    .build()
                NavigationLauncher.startNavigation(this@NavigationUIActivity, options)
            }
        }

        binding.simulateRouteSwitch.setOnCheckedChangeListener { _, checked ->
            simulateRoute = checked
        }


        binding.clearPoints.setOnClickListener {
            if (::mapboxMap.isInitialized) {
                mapboxMap.markers.forEach {
                    mapboxMap.removeMarker(it)
                }
            }
            destination = null
            waypoint = null
            it.visibility = View.GONE
            binding.startRouteLayout.visibility = View.GONE

            navigationMapRoute?.removeRoute()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(getString(R.string.map_style_light))) { style ->
            enableLocationComponent(style)
        }

        navigationMapRoute = NavigationMapRoute(
            binding.mapView,
            mapboxMap
        )

        mapboxMap.addOnMapClickListener(this)
        showSnackMessage("Tap map to place destination")
    }

    private fun showSnackMessage(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(
            findViewById(R.id.container),
            message,
            duration,
        ).show()
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        // Get an instance of the component
        locationComponent = mapboxMap.locationComponent

        locationComponent?.let {
            // Activate with a built LocationComponentActivationOptions object
            it.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, style).build(),
            )

            // Enable to make component visible
            it.isLocationComponentEnabled = true

            // Set the component's camera mode
            it.cameraMode = CameraMode.TRACKING_GPS_NORTH

            // Set the component's render mode
            it.renderMode = RenderMode.NORMAL
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        var addMarker = true
        when {
            destination == null -> destination = Point.fromLngLat(point.longitude, point.latitude)
            //waypoint == null -> waypoint = Point.fromLngLat(point.longitude, point.latitude)
            else -> {
                //Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
                addMarker = false
                return false
            }
        }
        showSnackMessage("loading route...", Snackbar.LENGTH_SHORT)

        if (addMarker) {
            mapboxMap.addMarker(MarkerOptions().position(point))
            binding.clearPoints.visibility = View.VISIBLE
        }
        calculateRoute()
        return true
    }

    private fun calculateRoute() {
        binding.startRouteLayout.visibility = View.GONE
        val userLocation = mapboxMap.locationComponent.lastKnownLocation
        val destination = destination
        if (userLocation == null) {
            Timber.d("calculateRoute: User location is null, therefore, origin can't be set.")
            return
        }

        if (destination == null) {
            return
        }

        val origin = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
            binding.startRouteLayout.visibility = View.GONE
            return
        }

        // call api
        val v = ValhallaClient()
        v.makeRequest(
            pickLat = userLocation.latitude,
            pickLon = userLocation.longitude,
            dropLat = destination.latitude(),
            dropLon = destination.longitude(),
            responseCallBack = { response ->
                Timber.d("Response: %s", response)
                runOnUiThread {
                    if (response.isEmpty()) return@runOnUiThread
                    val maplibreResponse = com.mapbox.services.android.navigation.v5.models.DirectionsResponse.fromJson(response)
                    val _customRoute = maplibreResponse.routes().first()
                    val directionsRoute = DirectionsRoute.builder()
                        .distance(_customRoute.distance())
                        .duration(_customRoute.duration())
                        .geometry(_customRoute.geometry())
                        .legs(_customRoute.legs())
                        .weightName(_customRoute.weightName())
                        .weight(_customRoute.weight())
                        .routeOptions(
                            RouteOptions
                            .builder()
                            .language("vi")
                            .accessToken("getString(R.string.mapbox_access_token)")
                            .alternatives(true)
                            .coordinates(listOf(
                                Point.fromLngLat(userLocation.longitude, userLocation.latitude),
                                Point.fromLngLat(destination.longitude(), destination.latitude())
                            ))
                            .geometries(com.mapbox.services.android.navigation.v5.models.DirectionsCriteria.GEOMETRY_POLYLINE6)
                            .profile(com.mapbox.services.android.navigation.v5.models.DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                            .steps(true)
                            .bannerInstructions(true)
                            .bearings(";")
                            .requestUuid("d")
                            .voiceUnits(DirectionsCriteria.METRIC)
                            .voiceInstructions(true)
                            .annotations("congestion,distance")
                            .overview("full")
                            .continueStraight(true)
                            .user("mapbox")
                            .roundaboutExits(true)
                            .baseUrl("getString(R.string.base_url)")
                            .build()
                        )
                        .build()
                    this@NavigationUIActivity.route = directionsRoute

                    navigationMapRoute?.addRoutes(maplibreResponse.routes())
                    binding.startRouteLayout.visibility = View.VISIBLE
                }

            }
        )
        return

        val navigationRouteBuilder = NavigationRoute.builder(this).apply {
            this.accessToken(getString(R.string.mapbox_access_token))
            this.origin(origin)
            this.destination(destination)
            this.voiceUnits(DirectionsCriteria.METRIC)
            this.alternatives(true)
            // If you are using this with the GraphHopper Directions API, you need to uncomment user and profile here.
            //this.user("gh")
            //this.profile("car")
            this.baseUrl(getString(R.string.base_url))
        }

        navigationRouteBuilder.build().getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>,
            ) {
                Timber.d("Url: %s", (call.request() as Request).url.toString())
                response.body()?.let { response ->
                    if (response.routes().isNotEmpty()) {
                        val maplibreResponse = com.mapbox.services.android.navigation.v5.models.DirectionsResponse.fromJson(response.toJson());
                        this@NavigationUIActivity.route = maplibreResponse.routes().first()
                        navigationMapRoute?.addRoutes(maplibreResponse.routes())
                        binding.startRouteLayout.visibility = View.VISIBLE
                    }
                }

            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Timber.e(throwable, "onFailure: navigation.getRoute()")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapboxMap.isInitialized) {
            mapboxMap.removeOnMapClickListener(this)
        }
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }
}
