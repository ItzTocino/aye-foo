package org.communityapp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.communityapp.dao.EventDao;
import org.junit.jupiter.api.Test;

class EventServiceTest {
    @Test
    void publishedTimeUsesEventZoneAcrossDstBoundary() {
        EventDao.Event winter = event("2026-01-15T15:00:00Z", "America/New_York", 8, 2);
        EventDao.Event summer = event("2026-07-15T14:00:00Z", "America/New_York", 8, 2);
        assertTrue(EventService.present(winter).get("when").contains("10:00 AM EST"));
        assertTrue(EventService.present(summer).get("when").contains("10:00 AM EDT"));
        assertEquals("6 spaces remaining", EventService.present(summer).get("availability"));
    }

    @Test
    void malformedZoneAndOverfilledRecordRemainSafe() {
        EventDao.Event invalid = event("2026-11-09T19:00:00Z", "Bad/Zone", 3, 8);
        assertTrue(EventService.present(invalid).get("when").contains("7:00 PM UTC"));
        assertEquals("Fully booked", EventService.present(invalid).get("availability"));
    }

    @Test
    void detailDisplaysLocalTimeCapacityAndUntrustedUrlSafely() {
        EventDao.Detail event = detail("2026-07-15T14:00:00Z", "America/New_York", "javascript:alert(1)", 0);
        var display = EventService.presentDetail(event);
        assertTrue(display.get("start").contains("10:00 AM EDT"));
        assertEquals("0 of 8 spaces remain", display.get("seats"));
        assertEquals("100", display.get("percent"));
        assertEquals("", display.get("mapUrl"));
        assertEquals("Park · Main St · Annapolis, MD, 21401", display.get("location"));
        assertEquals("https://maps.example.org/location",
                EventService.presentDetail(detail("2026-01-15T15:00:00Z", "America/New_York",
                        "https://maps.example.org/location", 5)).get("mapUrl"));
    }

    @Test
    void invalidEventMetadataDoesNotCrashRendering() {
        var display = EventService.presentDetail(detail("2026-11-09T19:00:00Z", "Bad/Zone",
                "https://user:pass@example.com/path", 99));
        assertTrue(display.get("start").contains("7:00 PM UTC"));
        assertEquals("8 of 8 spaces remain", display.get("seats"));
        assertEquals("0", display.get("percent"));
        assertEquals("", display.get("mapUrl"));
    }

    private EventDao.Detail detail(String start, String zone, String mapUrl, int seatsLeft) {
        return new EventDao.Detail("Cleanup", "Riverside", "Bring supplies", "Park", "Main St",
                "Annapolis", "MD", "21401", mapUrl, Instant.parse(start), null, zone, 8, seatsLeft, 2);
    }

    private EventDao.Event event(String start, String zone, int capacity, int taken) {
        return new EventDao.Event("Cleanup", "Riverside", "Park", "Annapolis", "MD",
                Instant.parse(start), zone, capacity, taken, "river-cleanup");
    }
}
