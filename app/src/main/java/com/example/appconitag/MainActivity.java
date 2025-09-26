package com.example.appconitag;

import android.content.Intent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // API URL
    private static final String API_URL = "https://oracleapex.com/ords/holdingtechsa/userAssert/auser";

    private ListView assetsListView;
    private AssetAdapter adapter;
    private ArrayList<AssetAdapter.Asset> assetList;

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL_MS = 30_000; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        assetsListView = findViewById(R.id.assetsListView);
        assetList = new ArrayList<>();
        adapter = new AssetAdapter(this, assetList);
        assetsListView.setAdapter(adapter);

        Button addButton = findViewById(R.id.addAssetButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open activity to add asset
                Intent intent = new Intent(MainActivity.this, AddAssetActivity.class);
                startActivity(intent);
            }
        });

        // Auto-refresh handler
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                new GetAssetsTask().execute();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };

        // Initial fetch and auto-refresh setup
        new GetAssetsTask().execute();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        // Clean up handler
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        super.onDestroy();
    }

    private class GetAssetsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                } else {
                    Log.e(TAG, "GET request failed: Response Code " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in GET request: " + e.getMessage());
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResponse = new JSONObject(result);
                JSONArray items = jsonResponse.getJSONArray("items");
                assetList.clear();

                // Get the assets directory and list all image files
                File assetsDir = new File(getFilesDir(), "assets");
                File[] imageFiles = assetsDir.exists() ? assetsDir.listFiles() : new File[0];

                for (int i = 0; i < items.length(); i++) {
                    JSONObject asset = items.getJSONObject(i);

                    int assetId = asset.getInt("asset_id");
                    String assetTag = asset.getString("asset_tag");
                    String assetName = asset.getString("asset_name");
                    String room = asset.getString("room");
                    String condition = asset.getString("condition");
                    String location = asset.getString("location");
                    String notes = asset.getString("notes");

                    // Find matching image file for this asset
                    String imagePath = findMatchingImage(assetName, assetTag, imageFiles);

                    AssetAdapter.Asset newAsset = new AssetAdapter.Asset(
                            assetId,
                            assetTag,
                            assetName,
                            room,
                            condition,
                            location,
                            notes,
                            imagePath
                    );
                    assetList.add(newAsset);
                }

                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.e(TAG, "Exception in parsing JSON: " + e.getMessage());
            }
        }

        // Helper method to find image file that matches asset name or tag
        private String findMatchingImage(String assetName, String assetTag, File[] imageFiles) {
            if (imageFiles == null || imageFiles.length == 0) return "";

            String searchName = assetName.toLowerCase().replace(" ", "_");
            String searchTag = assetTag.toLowerCase();

            for (File file : imageFiles) {
                String filename = file.getName().toLowerCase();
                if (filename.contains(searchName) || filename.contains(searchTag)) {
                    return file.getAbsolutePath();
                }
            }
            return "";
        }
    }
}