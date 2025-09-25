package com.example.appconitag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final String API_URL = "https://oracleapex.com/ords/holdingtechsa/Login/Auth/";

    // Fetch all username and password pairs from the API
    private Map<String, String> fetchUserCredentials() {
        Map<String, String> userCredentials = new HashMap<>();

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Process the JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray items = jsonResponse.getJSONArray("items");

                    // Store username and password pairs
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String username = item.getString("username");
                        String password = item.getString("password");
                        userCredentials.put(username, password);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userCredentials;
    }

    // Authenticate user credentials
    public boolean authenticate(String username, String password) {
        Map<String, String> userCredentials = fetchUserCredentials();
        String storedPassword = userCredentials.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
}
