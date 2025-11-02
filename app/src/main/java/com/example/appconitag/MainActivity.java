package com.example.appconitag;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "http://196.255.224.110:8003";
    private static final String API_URL = BASE_URL + "/api/assets";

    private ListView assetsListView;
    private AssetAdapter adapter;
    private ArrayList<AssetAdapter.Asset> assetList;
    private DatabaseHelper dbHelper;

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL_MS = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        assetsListView = findViewById(R.id.assetsListView);
        assetList = new ArrayList<>();
        adapter = new AssetAdapter(this, assetList);
        assetsListView.setAdapter(adapter);

        Button addButton = findViewById(R.id.addAssetButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAssetActivity.class);
            startActivity(intent);
        });

        Button syncButton = findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                List<Asset> unsyncedAssets = db.getUnsyncedAssets(); // get all assets with sync status = 0
                db.close();

                if (unsyncedAssets.isEmpty()) {
                    Toast.makeText(MainActivity.this, "All assets are already synced!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Asset asset : unsyncedAssets) {
                    syncWithServer(asset); // reuse your existing sync logic
                }

                Toast.makeText(MainActivity.this, "Syncing unsynced assets...", Toast.LENGTH_SHORT).show();
            }
        });

        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                new LoadLocalAssetsTask().execute();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };

        new LoadLocalAssetsTask().execute();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void syncWithServer(Asset asset) {
        apiService api = apiClient.getApiService(); // Get Retrofit service instance

        File imageFile = new File(asset.getImagePath());

        RequestBody assetTag = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getAssetTag());
        RequestBody assetName = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getAssetName());
        RequestBody room = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getRoom());
        RequestBody condition = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getCondition());
        RequestBody location = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getLocation());
        RequestBody notes = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getNotes());

        MultipartBody.Part picturePart = null;
        if (imageFile.exists()) {
            RequestBody imageBody = RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageFile);
            picturePart = MultipartBody.Part.createFormData("Picture", imageFile.getName(), imageBody);
        }

        // Call your API
        Call<ResponseBody> call = api.uploadAsset(assetTag, assetName, room, condition, location, notes, picturePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                    db.updateSyncStatus(asset.getId(), 1);
                    db.close();
                    Toast.makeText(MainActivity.this, "Asset synced successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Sync failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadLocalAssetsTask().execute();
    }

    @Override
    protected void onDestroy() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private class LoadLocalAssetsTask extends AsyncTask<Void, Void, List<Asset>> {
        @Override
        protected List<Asset> doInBackground(Void... voids) {
            List<Asset> assets = dbHelper.getAllAssets();
            Log.d(TAG, "Loaded " + assets.size() + " assets from local database");
            return assets;
        }

        @Override
        protected void onPostExecute(List<Asset> assets) {
            assetList.clear();

            File assetsDir = new File(getFilesDir(), "assets");
            File[] imageFiles = assetsDir.exists() ? assetsDir.listFiles() : new File[0];

            for (Asset asset : assets) {
                String imagePath = findMatchingImage(asset.getAssetName(), asset.getAssetTag(), imageFiles);

                if (imagePath.isEmpty() && asset.getImagePath() != null && !asset.getImagePath().isEmpty()) {
                    imagePath = asset.getImagePath();
                }

                AssetAdapter.Asset displayAsset = new AssetAdapter.Asset(
                        asset.getId(),
                        asset.getAssetTag(),
                        asset.getAssetName(),
                        asset.getRoom(),
                        asset.getCondition(),
                        asset.getLocation(),
                        asset.getNotes(),
                        imagePath
                );
                assetList.add(displayAsset);
            }

            adapter.notifyDataSetChanged();
            new SyncAndUpdateTask().execute();
        }

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

    private class SyncAndUpdateTask extends AsyncTask<Void, Void, Boolean> {
        private List<AssetAdapter.Asset> serverAssets = new ArrayList<>();

        @Override
        protected Boolean doInBackground(Void... voids) {
            syncLocalAssetsToServer();
            return fetchAssetsFromServer();
        }

        private void syncLocalAssetsToServer() {
            List<Asset> unsyncedAssets = dbHelper.getUnsyncedAssets();
            Log.d(TAG, "Syncing " + unsyncedAssets.size() + " unsynced assets to server");

            for (Asset asset : unsyncedAssets) {
                if (postAssetToServer(asset)) {
                    dbHelper.updateSyncStatus(asset.getId(), 1);
                    Log.d(TAG, "Successfully synced asset: " + asset.getAssetTag());
                } else {
                    Log.d(TAG, "Failed to sync asset: " + asset.getAssetTag());
                }
            }
        }

        private boolean postAssetToServer(Asset asset) {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("tag", asset.getAssetTag());
                jsonObject.put("name", asset.getAssetName());
                jsonObject.put("condition", asset.getCondition());
                jsonObject.put("location", asset.getLocation());
                jsonObject.put("notes", asset.getNotes());

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED;
            } catch (Exception e) {
                Log.e(TAG, "Error posting asset to server: " + e.getMessage());
                return false;
            }
        }

        private boolean fetchAssetsFromServer() {
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

                    JSONArray items = new JSONArray(result.toString());

                    File assetsDir = new File(getFilesDir(), "assets");
                    File[] imageFiles = assetsDir.exists() ? assetsDir.listFiles() : new File[0];

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject asset = items.getJSONObject(i);

                        int assetId = asset.getInt("id");
                        String assetTag = asset.getString("tag");
                        String assetName = asset.getString("name");
                        String condition = asset.getString("condition");
                        String location = asset.getString("location");
                        String notes = asset.optString("notes", "");

                        String imagePath = findMatchingImage(assetName, assetTag, imageFiles);

                        AssetAdapter.Asset serverAsset = new AssetAdapter.Asset(
                                assetId,
                                assetTag,
                                assetName,
                                "", // room - adjust based on your API
                                condition,
                                location,
                                notes,
                                imagePath
                        );
                        serverAssets.add(serverAsset);
                    }
                    return true;
                } else {
                    Log.e(TAG, "GET request failed: Response Code " + responseCode);
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in GET request: " + e.getMessage());
                return false;
            }
        }

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

        @Override
        protected void onPostExecute(Boolean success) {
            if (success && !serverAssets.isEmpty()) {
                Log.d(TAG, "Successfully fetched " + serverAssets.size() + " assets from server");
            }
        }
    }

    private void manualSync() {
        new SyncAndUpdateTask().execute();
        Toast.makeText(this, "Syncing with server...", Toast.LENGTH_SHORT).show();
    }
}