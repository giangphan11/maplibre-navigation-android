package com.mapbox.services.android.navigation.testapp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import org.json.JSONArray


typealias ResponseCallBack = (response: String) -> Unit

class ValhallaClient {

    private val client = OkHttpClient()

    fun makeRequest(
        // callBack response
        responseCallBack: ResponseCallBack,
        pickLat: Double,
        pickLon: Double,
        dropLat: Double,
        dropLon: Double
    ) {
        // Define the URL for the request
        val url = "https://valhalla1.openstreetmap.de/route"

        // Create JSON payload
        val jsonPayload = JSONObject().apply {
            put("costing", "auto")
            put("costing_options", JSONObject().apply {
                put("auto", JSONObject().apply {
                    put("maneuver_penalty", 5)
                    put("country_crossing_penalty", 0)
                    put("country_crossing_cost", 600)
                    put("width", 1.6)
                    put("height", 1.9)
                    put("use_highways", 1)
                    put("use_tolls", 1)
                    put("use_ferry", 1)
                    put("ferry_cost", 300)
                    put("use_living_streets", 0.5)
                    put("use_tracks", 0)
                    put("private_access_penalty", 450)
                    put("ignore_closures", false)
                    put("ignore_restrictions", false)
                    put("ignore_access", false)
                    put("closure_factor", 9)
                    put("service_penalty", 15)
                    put("service_factor", 1)
                    put("exclude_unpaved", 1)
                    put("shortest", false)
                    put("exclude_cash_only_tolls", false)
                    put("top_speed", 140)
                    put("fixed_speed", 0)
                    put("toll_booth_penalty", 0)
                    put("toll_booth_cost", 15)
                    put("gate_penalty", 300)
                    put("gate_cost", 30)
                    put("include_hov2", false)
                    put("include_hov3", false)
                    put("include_hot", false)
                    put("disable_hierarchy_pruning", false)
                })
            })
            put("exclude_polygons", JSONArray())
            put("locations", JSONArray().apply {
                put(JSONObject().apply {
                    put("lon", pickLon)
                    put("lat", pickLat)
                    put("type", "break")
                })
                put(JSONObject().apply {
                    put("lon", dropLon)
                    put("lat", dropLat)
                    put("type", "break")
                })
            })
            put("units", "kilometers")
            put("alternates", 0)
            put("id", "valhalla_directions")
            put("date_time", JSONObject().apply {
                put("type", 0)
            })
            put("directions_options", JSONObject().apply {
                put("language", "en")
            })
            put("format", "osrm")
            put("banner_instructions", true)
            put("voice_instructions", true)
        }

        // Create the RequestBody
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonPayload.toString()
        )

        // Build the request
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Accept-Language", "vi")
            .addHeader("Connection", "keep-alive")
            .addHeader("Origin", "https://valhalla.openstreetmap.de")
            .addHeader("Referer", "https://valhalla.openstreetmap.de/")
            .addHeader("Sec-Fetch-Dest", "empty")
            .addHeader("Sec-Fetch-Mode", "cors")
            .addHeader("Sec-Fetch-Site", "same-site")
            .addHeader("Sec-GPC", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Brave\";v=\"128\"")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-ch-ua-platform", "\"Windows\"")
            .addHeader("Authorization", "h")
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // Handle the error
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    responseCallBack(response.body?.string() ?: "")
                    // Handle the response
//                    val responseData = response.body?.string()
//                    //println("Response: $responseData")
//                    Timber.d("Vahala Response: %s", responseData)
                } else {
                    //println("Request failed with status code: ${response.code}")
                    responseCallBack( "")
                }
            }
        })
    }

}
