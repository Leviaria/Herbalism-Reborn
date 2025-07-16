package me.devupdates.herbalism.util;

public class TimeUtil {
    
    /**
     * Formats seconds into a human-readable time format
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (remainingSeconds > 0 || sb.length() == 0) {
            sb.append(remainingSeconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Formats milliseconds into a human-readable time format
     */
    public static String formatTimeMillis(long milliseconds) {
        return formatTime(milliseconds / 1000);
    }
    
    /**
     * Formats time with custom format strings
     */
    public static String formatTime(long seconds, String format) {
        if (seconds <= 0) {
            return "0s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        return format
                .replace("{days}", String.valueOf(days))
                .replace("{hours}", String.valueOf(hours))
                .replace("{minutes}", String.valueOf(minutes))
                .replace("{seconds}", String.valueOf(remainingSeconds));
    }
    
    /**
     * Formats time in the shortest possible way
     */
    public static String formatTimeShort(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else {
            return (seconds / 86400) + "d";
        }
    }
    
    /**
     * Converts time string to seconds
     */
    public static long parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0;
        }
        
        timeString = timeString.toLowerCase().trim();
        long totalSeconds = 0;
        
        // Support formats like "1h30m", "5m", "2h", "1d12h30m45s"
        String[] parts = timeString.split("(?<=\\d)(?=\\D)");
        
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            try {
                if (part.endsWith("d")) {
                    totalSeconds += Long.parseLong(part.substring(0, part.length() - 1)) * 86400;
                } else if (part.endsWith("h")) {
                    totalSeconds += Long.parseLong(part.substring(0, part.length() - 1)) * 3600;
                } else if (part.endsWith("m")) {
                    totalSeconds += Long.parseLong(part.substring(0, part.length() - 1)) * 60;
                } else if (part.endsWith("s")) {
                    totalSeconds += Long.parseLong(part.substring(0, part.length() - 1));
                } else {
                    // If no suffix, assume seconds
                    totalSeconds += Long.parseLong(part);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid parts
            }
        }
        
        return totalSeconds;
    }
    
    /**
     * Creates a progress bar for time
     */
    public static String createProgressBar(long current, long total, int length, String completedChar, String remainingChar) {
        if (total <= 0) {
            return remainingChar.repeat(length);
        }
        
        int completed = (int) ((current * length) / total);
        completed = Math.max(0, Math.min(completed, length));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < completed; i++) {
            sb.append(completedChar);
        }
        for (int i = completed; i < length; i++) {
            sb.append(remainingChar);
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a progress bar for time with default chars
     */
    public static String createProgressBar(long current, long total, int length) {
        return createProgressBar(current, total, length, "█", "░");
    }
    
    /**
     * Gets the current time in milliseconds
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * Gets the current time in seconds
     */
    public static long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }
    
    /**
     * Checks if a certain amount of time has passed
     */
    public static boolean hasTimePassed(long startTime, long duration) {
        return (getCurrentTimeMillis() - startTime) >= duration;
    }
    
    /**
     * Gets the remaining time in milliseconds
     */
    public static long getRemainingTime(long startTime, long duration) {
        long elapsed = getCurrentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }
    
    /**
     * Gets the remaining time in seconds
     */
    public static long getRemainingTimeSeconds(long startTime, long duration) {
        return getRemainingTime(startTime, duration) / 1000;
    }
    
    /**
     * Formats a timestamp into a readable date string
     */
    public static String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Never";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
} 