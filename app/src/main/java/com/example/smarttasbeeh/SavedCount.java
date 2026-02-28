package com.example.smarttasbeeh;

public class SavedCount {
    private final int id;
    private final String title;
    private final int count;
    private final String timestamp;
    private final long pinnedTimestamp;
    
    public SavedCount(int id, String title, int count, String timestamp, long pinnedTimestamp) {
        this.id = id;
        this.title = title;
        this.count = count;
        this.timestamp = timestamp;
        this.pinnedTimestamp = pinnedTimestamp;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getCount() { return count; }
    public String getTimestamp() { return timestamp; }

    public long getPinnedTimestamp() { return pinnedTimestamp; }
    public boolean isPinned() { return pinnedTimestamp > 0; }
}