package com.example.appconitag;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AssetAdapter extends ArrayAdapter<AssetAdapter.Asset> {
    private LayoutInflater inflater;

    public AssetAdapter(@NonNull Context context, @NonNull List<Asset> assetList) {
        super(context, 0, assetList);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Asset asset = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.asset_list_item, parent, false);
        }

        ImageView assetImage = convertView.findViewById(R.id.assetImage);
        TextView assetName = convertView.findViewById(R.id.assetName);
        TextView assetRoom = convertView.findViewById(R.id.assetRoom);
        TextView assetCondition = convertView.findViewById(R.id.assetCondition);
        TextView assetLocation = convertView.findViewById(R.id.assetLocation);
        TextView assetNotes = convertView.findViewById(R.id.assetNotes);

        // Set data
        assetName.setText("Name: " + asset.assetName);
        assetRoom.setText("Room: " + asset.room);
        assetCondition.setText("Condition: " + asset.condition);
        assetLocation.setText("Location: " + asset.location);
        assetNotes.setText("Notes: " + asset.notes);

        // Placeholder image (you can load real image if available)
        assetImage.setImageResource(R.drawable.ic_launcher_foreground);

        return convertView;
    }

    // Inner Model class
    public static class Asset {
        public int assetId;
        public String assetTag, assetName, room, condition, location, notes;

        public Asset(int assetId, String assetTag, String assetName, String room,
                     String condition, String location, String notes) {
            this.assetId = assetId;
            this.assetTag = assetTag;
            this.assetName = assetName;
            this.room = room;
            this.condition = condition;
            this.location = location;
            this.notes = notes;
        }
    }
}
