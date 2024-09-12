package com.mapbox.services.android.navigation.ui.v5.route;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class ValhallaClient {

    private final OkHttpClient client = new OkHttpClient();

    // Define a callback interface for responses
    public interface ResponseCallBack {
        void onResponse(String response);
    }

    public void makeRequest(
            final ResponseCallBack responseCallBack,
            double pickLat, double pickLon,
            double dropLat, double dropLon
    ) throws Exception {
        // Define the URL for the request
        String url = "https://valhalla1.openstreetmap.de/route";

        // Create JSON payload
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("costing", "auto");

        JSONObject costingOptions = new JSONObject();
        JSONObject auto = new JSONObject();
        auto.put("maneuver_penalty", 5);
        auto.put("country_crossing_penalty", 0);
        auto.put("country_crossing_cost", 600);
        auto.put("width", 1.6);
        auto.put("height", 1.9);
        auto.put("use_highways", 1);
        auto.put("use_tolls", 1);
        auto.put("use_ferry", 1);
        auto.put("ferry_cost", 300);
        auto.put("use_living_streets", 0.5);
        auto.put("use_tracks", 0);
        auto.put("private_access_penalty", 450);
        auto.put("ignore_closures", false);
        auto.put("ignore_restrictions", false);
        auto.put("ignore_access", false);
        auto.put("closure_factor", 9);
        auto.put("service_penalty", 15);
        auto.put("service_factor", 1);
        auto.put("exclude_unpaved", 1);
        auto.put("shortest", false);
        auto.put("exclude_cash_only_tolls", false);
        auto.put("top_speed", 140);
        auto.put("fixed_speed", 0);
        auto.put("toll_booth_penalty", 0);
        auto.put("toll_booth_cost", 15);
        auto.put("gate_penalty", 300);
        auto.put("gate_cost", 30);
        auto.put("include_hov2", false);
        auto.put("include_hov3", false);
        auto.put("include_hot", false);
        auto.put("disable_hierarchy_pruning", false);

        costingOptions.put("auto", auto);
        jsonPayload.put("costing_options", costingOptions);
        jsonPayload.put("exclude_polygons", new JSONArray());

        JSONArray locations = new JSONArray();
        JSONObject pickLocation = new JSONObject();
        pickLocation.put("lon", pickLon);
        pickLocation.put("lat", pickLat);
        pickLocation.put("type", "break");
        locations.put(pickLocation);

        JSONObject dropLocation = new JSONObject();
        dropLocation.put("lon", dropLon);
        dropLocation.put("lat", dropLat);
        dropLocation.put("type", "break");
        locations.put(dropLocation);

        jsonPayload.put("locations", locations);
        jsonPayload.put("units", "kilometers");
        jsonPayload.put("alternates", 0);
        jsonPayload.put("id", "valhalla_directions");

        JSONObject dateTime = new JSONObject();
        dateTime.put("type", 0);
        jsonPayload.put("date_time", dateTime);

        JSONObject directionsOptions = new JSONObject();
        directionsOptions.put("language", "en");
        jsonPayload.put("directions_options", directionsOptions);

        jsonPayload.put("format", "osrm");
        jsonPayload.put("banner_instructions", true);
        jsonPayload.put("voice_instructions", true);

        // Create the RequestBody
        RequestBody requestBody = RequestBody.create(
                jsonPayload.toString(), MediaType.parse("application/json; charset=utf-8"));

        // Build the request
        Request request = new Request.Builder()
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
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace(); // Handle the error
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // check main thread

                    responseCallBack.onResponse(response.body().string());
                } else {
                    responseCallBack.onResponse("");
                }
            }
        });
    }
}

