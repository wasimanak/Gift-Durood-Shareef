package com.alpha4technologies.duroodcounter.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String userId;
    private String username;
    private String fullName;
    private String city;
    private String country;
    private boolean isGuest;
    private long totalCount;
    private long todayCount;
    private String lastDate;
    private long lastUpdated;

    public User() {
        // Required empty constructor for Firebase Realtime Database
    }

    public User(String userId, String username, String fullName, String city, String country,
                boolean isGuest, long totalCount, long todayCount, String lastDate, long lastUpdated) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.city = city;
        this.country = country;
        this.isGuest = isGuest;
        this.totalCount = totalCount;
        this.todayCount = todayCount;
        this.lastDate = lastDate;
        this.lastUpdated = lastUpdated;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public void setGuest(boolean guest) {
        isGuest = guest;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getTodayCount() {
        return todayCount;
    }

    public void setTodayCount(long todayCount) {
        this.todayCount = todayCount;
    }

    public String getLastDate() {
        return lastDate;
    }

    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
