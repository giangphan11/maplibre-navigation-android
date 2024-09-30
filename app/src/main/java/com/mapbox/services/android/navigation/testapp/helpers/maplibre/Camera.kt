package com.mapbox.services.android.navigation.testapp.helpers.maplibre

import android.content.ContentResolver
import android.provider.Settings
import androidx.core.graphics.Insets
import com.mapbox.geojson.BoundingBox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap


var MapboxMap.camera: CameraPosition
    get() = cameraPosition.toCameraPosition()
    set(value) { cameraPosition = value.toMapLibreCameraPosition() }

fun MapboxMap.updateCamera(duration: Int = 0, contentResolver: ContentResolver, builder: CameraUpdate.() -> Unit) {
    val update = CameraUpdate().apply(builder).toMapLibreCameraUpdate(camera)
    val animatorScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    if (duration == 0 || animatorScale == 0f) {
        moveCamera(update)
    } else {
        easeCamera(update, (duration * animatorScale).toInt())
    }
}

/** Builder data class for camera updates */
class CameraUpdate {
    var position: LatLng? = null
    var rotation: Double? = null // degrees
    var tilt: Double? = null // degrees
    var zoom: Double? = null
    var padding: Padding? = null

    var zoomBy: Double? = null
    var tiltBy: Double? = null
    var rotationBy: Double? = null
}

private fun CameraUpdate.toMapLibreCameraUpdate(cameraPosition: CameraPosition): com.mapbox.mapboxsdk.camera.CameraUpdate {
    resolveDeltas(cameraPosition)
    val builder = com.mapbox.mapboxsdk.camera.CameraPosition.Builder(cameraPosition.toMapLibreCameraPosition())
    rotation?.let { builder.bearing(it) }
    position?.let { builder.target(it.toLatLng()) }
    zoom?.let { builder.zoom(it) }
    tilt?.let { builder.tilt(it) }
    padding?.let { builder.padding(it.toDoubleArray()) }
    return CameraUpdateFactory.newCameraPosition(builder.build())
}

private fun CameraUpdate.resolveDeltas(pos: CameraPosition) {
    zoomBy?.let { zoom = pos.zoom + (zoom ?: 0.0) + it }
    tiltBy?.let { tilt = pos.tilt + (tilt ?: 0.0) + it }
    rotationBy?.let { rotation = pos.rotation + (rotation ?: 0.0) + it }
}

/** State of the camera */
data class CameraPosition(
    val position: LatLng,
    val rotation: Double,
    val tilt: Double,
    val zoom: Double,
    val padding: Padding? = null
)
data class Padding(val left: Double, val top: Double, val right: Double, val bottom: Double)

private fun com.mapbox.mapboxsdk.camera.CameraPosition.toCameraPosition() = CameraPosition(
    position = target?.toLatLon() ?: LatLng(0.0, 0.0),
    rotation = bearing,
    tilt = tilt,
    zoom = zoom,
    padding = padding?.toPadding()
)

private fun CameraPosition.toMapLibreCameraPosition(): com.mapbox.mapboxsdk.camera.CameraPosition =
    com.mapbox.mapboxsdk.camera.CameraPosition.Builder()
        .bearing(rotation)
        .zoom(zoom)
        .tilt(tilt)
        .target(position.toLatLng())
        .padding(padding?.toDoubleArray())
        .build()

private fun Padding.toDoubleArray() = doubleArrayOf(left, top, right, bottom)
private fun DoubleArray.toPadding() = Padding(this[0], this[1], this[2], this[3])

fun Insets.toPadding() = Padding(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())