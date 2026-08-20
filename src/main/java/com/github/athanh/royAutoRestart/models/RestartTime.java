package com.github.athanh.royAutoRestart.models;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

/**
 * Model representing a configured restart time.
 * Supports daily formats (DAILY;12:00, EVERYDAY;12:00, ALL;12:00)
 * and day-of-week formats (MONDAY;11:00, etc.).
 */
public class RestartTime {
    private final String day;
    private final boolean isDaily;
    private final int hour;
    private final int minute;

    public RestartTime(String line) throws IllegalArgumentException {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule entry cannot be empty");
        }

        String[] parts = line.split(";");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format. Expected 'DAY;HH:mm' or 'DAILY;HH:mm', got: " + line);
        }

        this.day = parts[0].trim().toUpperCase();
        this.isDaily = this.day.equals("DAILY") || this.day.equals("EVERYDAY") || this.day.equals("ALL");

        // Validate day name if not daily
        if (!isDaily) {
            try {
                DayOfWeek.valueOf(this.day);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid day name: " + parts[0] + ". Use DAILY or MONDAY-SUNDAY.");
            }
        }

        String[] timeParts = parts[1].trim().split(":");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Invalid time format: " + parts[1] + ". Expected HH:mm");
        }

        try {
            this.hour = Integer.parseInt(timeParts[0].trim());
            this.minute = Integer.parseInt(timeParts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Time values out of range: " + parts[1] + " (Hour: 0-23, Minute: 0-59)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time numbers: " + parts[1]);
        }
    }

    public String getDay() {
        return day;
    }

    public boolean isDaily() {
        return isDaily;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    /**
     * Check if the given LocalDateTime matches this schedule.
     */
    public boolean isMatch(LocalDateTime now) {
        if (now.getHour() != this.hour || now.getMinute() != this.minute) {
            return false;
        }
        if (isDaily) {
            return true;
        }
        return now.getDayOfWeek().name().equalsIgnoreCase(this.day);
    }

    /**
     * Calculate the next occurrence LocalDateTime from a reference point.
     */
    public LocalDateTime getNextOccurrence(LocalDateTime now) {
        LocalDateTime target = now
                .with(ChronoField.HOUR_OF_DAY, this.hour)
                .with(ChronoField.MINUTE_OF_HOUR, this.minute)
                .with(ChronoField.SECOND_OF_MINUTE, 0)
                .with(ChronoField.NANO_OF_SECOND, 0);

        if (isDaily) {
            if (!target.isAfter(now)) {
                target = target.plusDays(1);
            }
            return target;
        }

        DayOfWeek targetDay = DayOfWeek.valueOf(this.day);
        int currentDayValue = now.getDayOfWeek().getValue();
        int targetDayValue = targetDay.getValue();

        int daysUntil = targetDayValue - currentDayValue;
        if (daysUntil < 0 || (daysUntil == 0 && !target.isAfter(now))) {
            daysUntil += 7;
        }

        return target.plusDays(daysUntil);
    }

    @Override
    public String toString() {
        return (isDaily ? "DAILY" : day) + ";" + String.format("%02d:%02d", hour, minute);
    }
}