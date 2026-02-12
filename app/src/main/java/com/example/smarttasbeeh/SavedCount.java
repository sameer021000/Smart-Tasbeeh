package com.example.smarttasbeeh;

public class SavedCount {
    private int id;
    private String title;
    private int count;
    private String timestamp;

    public SavedCount(int id, String title, int count, String timestamp) {
        this.id = id;
        this.title = title;
        this.count = count;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getCount() { return count; }
    public String getTimestamp() { return timestamp; }
}