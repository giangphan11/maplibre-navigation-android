package com.mapbox.services.android.navigation.testapp.helpers.maplibre

import android.location.Location
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds


fun LatLng.toPoint(): Point = Point.fromLngLat(longitude, latitude)
fun Point.toLatLon(): LatLng = LatLng(latitude(), longitude())

fun GeoJsonSource.clear() = setGeoJson(null as FeatureCollection?)

fun Location.toLatLon() = LatLng(latitude, longitude)


val Location.elapsedDuration: Duration get() = elapsedRealtimeNanos.nanoseconds

fun LatLng.toLatLon() = LatLng(latitude, longitude)
fun LatLng.toLatLng() = LatLng(latitude, longitude)