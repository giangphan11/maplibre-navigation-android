package com.mapbox.services.android.navigation.testapp.helpers

import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.division
import com.mapbox.mapboxsdk.style.expressions.Expression.exponential
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.product
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import kotlin.math.PI

private fun Double.toRadians() = this / 180.0 * PI
private fun Double.toDegrees() = this / PI * 180.0

fun normalizeLongitude(lon: Double): Double {
    var normalizedLon = lon % 360 // normalizedLon is -360..360
    if (normalizedLon < -180) {
        normalizedLon += 360
    } else if (normalizedLon >= 180) {
        normalizedLon -= 360
    }
    return normalizedLon
}

fun inMeters(expression: Expression, latitude: Double = 30.0): Expression {
    // the more north you go, the smaller of an area each mercator tile actually covers
    // the additional factor of 1.20 comes from a simple measuring test with a ruler on a
    // smartphone screen done at approx. latitude = 0 and latitude = 70, i.e. without it, lines are
    // drawn at both latitudes approximately 20% too large
    val sizeFactor = kotlin.math.cos(PI * latitude / 180) * 1.2
    return interpolate(
        exponential(2), zoom(),
        stop(8, division(division(expression, literal(256)), literal(sizeFactor))),
        stop(24, division(product(expression, literal(256)), literal(sizeFactor)))
    )
}