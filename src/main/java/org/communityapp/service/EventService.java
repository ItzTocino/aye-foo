package org.communityapp.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.communityapp.dao.EventDao;

/** Formats database-backed public event listings for the calendar. */
public final class EventService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a z");
    private final EventDao dao;
    private final Clock clock;

    public EventService(EventDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    public List<Map<String, String>> upcoming() throws java.sql.SQLException {
        return dao.upcoming(clock.instant()).stream().map(EventService::present).toList();
    }

    /** A null result means missing, past or unpublished, indistinguishable to public visitors. */
    public Map<String, String> detail(String slug) throws java.sql.SQLException {
        if (slug == null || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() > 180) {
            return null;
        }
        EventDao.Detail event = dao.findUpcomingPublished(slug, clock.instant());
        return event == null ? null : presentDetail(event);
    }

    public static Map<String, String> presentDetail(EventDao.Detail event) {
        ZoneId zone = eventZone(event.timezone());
        String locality = String.join(", ", java.util.stream.Stream.of(event.city(), event.state(), event.zip())
                .filter(value -> value != null && !value.isBlank()).toList());
        String location = String.join(" · ", java.util.stream.Stream.of(event.location(), event.address(), locality)
                .filter(value -> value != null && !value.isBlank()).toList());
        int capacity = Math.max(1, event.capacity());
        int left = Math.max(0, Math.min(capacity, event.seatsLeft()));
        String map = validMapUrl(event.mapUrl());
        return Map.of("title", safe(event.title()), "summary", safe(event.summary()),
                "description", safe(event.description()), "location", location,
                "mapUrl", map, "start", DATE.format(event.startsAt().atZone(zone)),
                "end", event.endsAt() == null ? "" : DATE.format(event.endsAt().atZone(zone)),
                "seats", left + " of " + capacity + " spaces remain",
                "percent", Integer.toString((int) (100L * (capacity - left) / capacity)),
                "waitlist", Integer.toString(Math.max(0, event.waitlistCount())));
    }

    private static String validMapUrl(String candidate) {
        if (candidate == null || candidate.length() > 500) {
            return "";
        }
        try {
            URI uri = URI.create(candidate);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && !uri.getHost().isBlank() && uri.getRawUserInfo() == null
                    && uri.getPort() == -1) {
                return candidate;
            }
        } catch (IllegalArgumentException ignored) {
            // An untrusted stored URL must never become a public hyperlink.
        }
        return "";
    }

    public static Map<String, String> present(EventDao.Event event) {
        ZoneId zone = eventZone(event.timezone());
        String place = safe(event.location());
        String locality = String.join(", ", java.util.stream.Stream.of(event.city(), event.state())
                .filter(value -> value != null && !value.isBlank()).toList());
        if (!locality.isEmpty()) {
            place = place.isBlank() ? locality : place + " - " + locality;
        }
        int seatsLeft = Math.max(0, event.capacity() - event.seatsTaken());
        return Map.of("title", safe(event.title()), "summary", safe(event.summary()),
                "place", place, "when", DATE.format(event.startsAt().atZone(zone)),
                "availability", seatsLeft == 0 ? "Fully booked" : seatsLeft + " spaces remaining",
                "slug", safe(event.slug()).matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                        && safe(event.slug()).length() <= 180 ? event.slug() : "");
    }

    private static ZoneId eventZone(String name) {
        try {
            return ZoneId.of(name);
        } catch (DateTimeException | NullPointerException invalidZone) {
            return ZoneId.of("UTC");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
