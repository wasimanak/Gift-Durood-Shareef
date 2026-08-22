package com.alpha4technologies.duroodcounter.model;

public class RecitationSession {
    private String sessionId;
    private String userId;
    private long count;
    private long syncedCount;
    private String date;
    private long timestamp;
    private boolean synced;

    public RecitationSession() {
    }

    public RecitationSession(String sessionId, String userId, long count, long syncedCount, String date, long timestamp, boolean synced) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.count = count;
        this.syncedCount = syncedCount;
        this.date = date;
        this.timestamp = timestamp;
        this.synced = synced;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(long syncedCount) {
        this.syncedCount = syncedCount;
    }

    public long getUnsyncedDelta() {
        return Math.max(0, count - syncedCount);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
