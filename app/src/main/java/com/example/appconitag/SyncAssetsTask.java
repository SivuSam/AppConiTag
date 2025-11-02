package com.example.appconitag;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SyncAssetsTask extends AsyncTask<Void, Void, Boolean> {

    private Context context;
    private static final String POST_ASSET_URL = "https://oracleapex.com/ords/holdingtechsa/admin/Assert/allassert";

    public SyncAssetsTask(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Asset> unsyncedAssets = dbHelper.getUnsyncedAssets();

        boolean allSynced = true;

        for (Asset asset : unsyncedAssets) {
            if (postAssetToServer(asset)) {
                dbHelper.updateSyncStatus(asset.getId(), 1); // Mark as synced
            } else {
                allSynced = false;
            }
        }

        dbHelper.close();
        return allSynced;
    }

    private boolean postAssetToServer(Asset asset) {
        try {
            URL url = new URL(POST_ASSET_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("asset_tag", asset.getAssetTag());
            jsonObject.put("asset_name", asset.getAssetName());
            jsonObject.put("room", asset.getRoom());
            jsonObject.put("condition", asset.getCondition());
            jsonObject.put("location", asset.getLocation());
            jsonObject.put("notes", asset.getNotes());
            jsonObject.put("verified", asset.getVerified());
            jsonObject.put("submitted_by", asset.getSubmittedBy());
            jsonObject.put("image_path", asset.getImagePath());

            OutputStream os = conn.getOutputStream();
            os.write(jsonObject.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean allSynced) {
        if (allSynced) {
            Log.d("Sync", "All assets synced successfully");
        } else {
            Log.d("Sync", "Some assets failed to sync - will retry later");
        }
    }
}