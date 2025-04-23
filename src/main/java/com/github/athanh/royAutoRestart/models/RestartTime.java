package com.github.athanh.royAutoRestart.models;

public class RestartTime {
    private final String day;
    private final int hour;
    private final int minute;

    public RestartTime(String line) throws IllegalArgumentException {
        String[] parts = line.split(";");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format: " + line);
        }

        this.day = parts[0].trim();
        String[] timeParts = parts[1].trim().split(":");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Invalid time format: " + parts[1]);
        }

        try {
            this.hour = Integer.parseInt(timeParts[0]);
            this.minute = Integer.parseInt(timeParts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid time values");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time numbers: " + parts[1]);
        }
    }

    public String getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }
}