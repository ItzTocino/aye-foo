package org.communityapp.service;

import java.util.List;
import java.util.Map;
import org.communityapp.dao.ContentDao;

/** Adapts cached public read models to JSP-compatible maps. */
public final class ContentService {
    private final ContentDao dao;

    public ContentService(ContentDao dao) {
        this.dao = dao;
    }

    public Content publicContent() throws java.sql.SQLException {
        ContentDao.Snapshot snapshot = dao.publicContent();
        List<Map<String, String>> initiatives = snapshot.initiatives().stream()
                .map(item -> Map.of("title", safe(item.title()), "summary", safe(item.summary()),
                        "body", safe(item.body()), "location", safe(item.location())))
                .toList();
        List<Map<String, String>> partners = snapshot.partners().stream()
                .map(item -> Map.of("name", safe(item.name()), "blurb", safe(item.blurb()),
                        "websiteUrl", safe(item.websiteUrl())))
                .toList();
        return new Content(initiatives, partners);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record Content(List<Map<String, String>> initiatives, List<Map<String, String>> partners) { }
}
