package com.example.appconitag;

public class Asset {
    private int id;
    private String assetTag;
    private String assetName;
    private String room;
    private String condition;
    private String location;
    private String notes;
    private String verified;
    private String imagePath;
    private int submittedBy;
    private int syncStatus; // 0 = not synced, 1 = synced

    // Constructors
    public Asset() {}

    public Asset(String assetTag, String assetName, String room, String condition,
                 String location, String notes, String verified, String imagePath, int submittedBy) {
        this.assetTag = assetTag;
        this.assetName = assetName;
        this.room = room;
        this.condition = condition;
        this.location = location;
        this.notes = notes;
        this.verified = verified;
        this.imagePath = imagePath;
        this.submittedBy = submittedBy;
        this.syncStatus = 0; // Default to not synced
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getVerified() { return verified; }
    public void setVerified(String verified) { this.verified = verified; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(int submittedBy) { this.submittedBy = submittedBy; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
}