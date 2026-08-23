package com.github.athanh.royAutoRestart;

import com.github.athanh.royAutoRestart.models.RestartTime;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class RestartTimeTest {

    @Test
    public void testDailyRestartTimeParsing() {
        RestartTime rt = new RestartTime("DAILY;12:00");
        assertTrue(rt.isDaily());
        assertEquals("DAILY", rt.getDay());
        assertEquals(12, rt.getHour());
        assertEquals(0, rt.getMinute());
    }

    @Test
    public void testEverydayAliasParsing() {
        RestartTime rt = new RestartTime("EVERYDAY;00:00");
        assertTrue(rt.isDaily());
        assertEquals(0, rt.getHour());
        assertEquals(0, rt.getMinute());
    }

    @Test
    public void testDayOfWeekRestartTimeParsing() {
        RestartTime rt = new RestartTime("MONDAY;11:30");
        assertFalse(rt.isDaily());
        assertEquals("MONDAY", rt.getDay());
        assertEquals(11, rt.getHour());
        assertEquals(30, rt.getMinute());
    }

    @Test
    public void testInvalidFormats() {
        assertThrows(IllegalArgumentException.class, () -> new RestartTime("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> new RestartTime("SOMEDAY;12:00"));
        assertThrows(IllegalArgumentException.class, () -> new RestartTime("DAILY;25:00"));
        assertThrows(IllegalArgumentException.class, () -> new RestartTime("DAILY;12:60"));
        assertThrows(IllegalArgumentException.class, () -> new RestartTime("DAILY;ab:cd"));
    }

    @Test
    public void testDailyNextOccurrence() {
        RestartTime rt = new RestartTime("DAILY;12:00");

        // Case 1: reference time is today at 10:00 (before 12:00)
        LocalDateTime before = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime next = rt.getNextOccurrence(before);
        assertEquals(2026, next.getYear());
        assertEquals(8, next.getMonthValue());
        assertEquals(20, next.getDayOfMonth());
        assertEquals(12, next.getHour());
        assertEquals(0, next.getMinute());

        // Case 2: reference time is today at 14:00 (after 12:00)
        LocalDateTime after = LocalDateTime.of(2026, 8, 20, 14, 0);
        LocalDateTime nextDay = rt.getNextOccurrence(after);
        assertEquals(2026, nextDay.getYear());
        assertEquals(8, nextDay.getMonthValue());
        assertEquals(21, nextDay.getDayOfMonth());
        assertEquals(12, nextDay.getHour());
        assertEquals(0, nextDay.getMinute());
    }

    @Test
    public void testDayOfWeekNextOccurrence() {
        RestartTime rt = new RestartTime("FRIDAY;18:00");
        // Thursday 2026-08-20 at 10:00
        LocalDateTime thursday = LocalDateTime.of(2026, 8, 20, 10, 0);
        assertEquals(DayOfWeek.THURSDAY, thursday.getDayOfWeek());

        LocalDateTime next = rt.getNextOccurrence(thursday);
        assertEquals(DayOfWeek.FRIDAY, next.getDayOfWeek());
        assertEquals(21, next.getDayOfMonth());
        assertEquals(18, next.getHour());
        assertEquals(0, next.getMinute());
    }

    @Test
    public void testIsMatch() {
        RestartTime daily = new RestartTime("DAILY;12:00");
        LocalDateTime matchTime = LocalDateTime.of(2026, 8, 20, 12, 0);
        LocalDateTime nonMatchTime = LocalDateTime.of(2026, 8, 20, 12, 1);

        assertTrue(daily.isMatch(matchTime));
        assertFalse(daily.isMatch(nonMatchTime));
    }

    @Test
    public void testVietnamTimeZoneMatchOnUtcServer() {
        // Assume the physical VPS is located in Germany/UK running in UTC
        // When it is 05:00 UTC on 2026-08-23, in Vietnam (GMT+7) it is exactly 12:00 PM!
        Instant nowUtc = Instant.parse("2026-08-23T05:00:00Z");
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime vnTime = nowUtc.atZone(vnZone);

        assertEquals(12, vnTime.getHour());
        assertEquals(0, vnTime.getMinute());

        RestartTime dailySchedule = new RestartTime("DAILY;12:00");
        assertTrue(dailySchedule.isMatch(vnTime.toLocalDateTime()), "DAILY;12:00 should match 05:00 UTC when converted to Vietnam Time");
    }

    @Test
    public void testCustomForeignTimezones() {
        // Test New York (UTC-4 in summer DST)
        Instant nowUtc = Instant.parse("2026-08-23T16:00:00Z");
        ZoneId nyZone = ZoneId.of("America/New_York");
        ZonedDateTime nyTime = nowUtc.atZone(nyZone);

        assertEquals(12, nyTime.getHour());
        assertEquals(0, nyTime.getMinute());

        RestartTime nySchedule = new RestartTime("DAILY;12:00");
        assertTrue(nySchedule.isMatch(nyTime.toLocalDateTime()), "DAILY;12:00 should match New York local time");
    }
}
