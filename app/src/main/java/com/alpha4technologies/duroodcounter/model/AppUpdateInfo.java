package com.alpha4technologies.duroodcounter.model;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class AppUpdateInfo {
    private long latestVersionCode;
    private String latestVersionName;
    private boolean forceUpdate;
    private String updateUrl;
    private String updateTitle;
    private String updateMessage;

    public AppUpdateInfo() {
        // Required empty constructor for Firebase
    }

    public AppUpdateInfo(long latestVersionCode, String latestVersionName, boolean forceUpdate, String updateUrl, String updateTitle, String updateMessage) {
        this.latestVersionCode = latestVersionCode;
        this.latestVersionName = latestVersionName;
        this.forceUpdate = forceUpdate;
        this.updateUrl = updateUrl;
        this.updateTitle = updateTitle;
        this.updateMessage = updateMessage;
    }

    public static AppUpdateInfo fromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return null;

        AppUpdateInfo info = new AppUpdateInfo();

        Object codeObj = getChildValue(snapshot, "latest_version_code", "latestVersionCode", "version_code", "versionCode");
        info.setLatestVersionCode(parseLong(codeObj));

        info.setLatestVersionName(getChildString(snapshot, "latest_version_name", "latestVersionName", "version_name", "versionName"));

        Object forceObj = getChildValue(snapshot, "force_update", "forceUpdate", "is_force_update", "isForceUpdate");
        info.setForceUpdate(parseBoolean(forceObj));

        info.setUpdateUrl(getChildString(snapshot, "update_url", "updateUrl", "url"));
        info.setUpdateTitle(getChildString(snapshot, "update_title", "updateTitle", "title"));
        info.setUpdateMessage(getChildString(snapshot, "update_message", "updateMessage", "message"));

        return info;
    }

    public static boolean isUpdateRequired(long currentVersionCode, String currentVersionName, AppUpdateInfo updateInfo) {
        if (updateInfo == null) return false;

        long latestCode = updateInfo.getLatestVersionCode();
        String latestName = updateInfo.getLatestVersionName();

        // 1. Check version code first
        if (latestCode > currentVersionCode) {
            return true;
        }

        // 2. If version code is same or less, check semantic version name if available
        if (latestCode == currentVersionCode && latestName != null && !latestName.trim().isEmpty()) {
            return isVersionNameHigher(latestName.trim(), currentVersionName);
        }

        return false;
    }

    private static boolean isVersionNameHigher(String latestVersion, String currentVersion) {
        if (latestVersion == null || currentVersion == null) return false;
        if (latestVersion.equals(currentVersion)) return false;

        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? parseInteger(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseInteger(currentParts[i]) : 0;

            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }

        return false;
    }

    private static int parseInteger(String val) {
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static Object getChildValue(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            if (snapshot.child(key).exists() && snapshot.child(key).getValue() != null) {
                return snapshot.child(key).getValue();
            }
        }
        return null;
    }

    private static String getChildString(DataSnapshot snapshot, String... keys) {
        Object val = getChildValue(snapshot, keys);
        return val != null ? String.valueOf(val) : "";
    }

    private static long parseLong(Object val) {
        if (val == null) return 1L;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            String s = ((String) val).trim();
            try {
                if (s.contains(".")) {
                    return (long) Double.parseDouble(s);
                }
                return Long.parseLong(s);
            } catch (Exception e) {
                return 1L;
            }
        }
        return 1L;
    }

    private static boolean parseBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) {
            String s = ((String) val).trim().toLowerCase();
            return "true".equals(s) || "1".equals(s) || "yes".equals(s);
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return false;
    }

    public long getLatestVersionCode() {
        return latestVersionCode;
    }

    public void setLatestVersionCode(long latestVersionCode) {
        this.latestVersionCode = latestVersionCode;
    }

    public void setLatestVersionCode(Object val) {
        this.latestVersionCode = parseLong(val);
    }

    public String getLatestVersionName() {
        return latestVersionName;
    }

    public void setLatestVersionName(String latestVersionName) {
        this.latestVersionName = latestVersionName;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public void setForceUpdate(Object val) {
        this.forceUpdate = parseBoolean(val);
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getUpdateTitle() {
        return updateTitle;
    }

    public void setUpdateTitle(String updateTitle) {
        this.updateTitle = updateTitle;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public void setUpdateMessage(String updateMessage) {
        this.updateMessage = updateMessage;
    }
}
