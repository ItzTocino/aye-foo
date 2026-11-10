package org.communityapp.web;

import java.io.Serializable;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, single-use form tokens scoped to one browser session and event. */
final class RegistrationTokens implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_FORMS = 8;
    private final Map<String, Form> forms = new LinkedHashMap<>();

    synchronized void issue(String token, String slug, Instant issuedAt) {
        forms.entrySet().removeIf(entry -> entry.getValue().issuedAt().plusSeconds(7200)
                .isBefore(issuedAt));
        forms.put(token, new Form(slug, issuedAt));
        while (forms.size() > MAX_FORMS) {
            Iterator<String> oldest = forms.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /** Consumes even a token submitted for the wrong event, preventing replay. */
    synchronized Instant consume(String token, String slug) {
        Form form = token == null ? null : forms.remove(token);
        return form != null && form.slug().equals(slug) ? form.issuedAt() : null;
    }

    private record Form(String slug, Instant issuedAt) implements Serializable { }
}
