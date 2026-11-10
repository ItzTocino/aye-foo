package org.communityapp.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RegistrationTokensTest {
    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    @Test
    void openingAnotherEventDoesNotInvalidateEarlierTab() {
        RegistrationTokens forms = new RegistrationTokens();
        forms.issue("first", "cleanup-a", NOW);
        forms.issue("second", "cleanup-b", NOW.plusSeconds(3));
        assertEquals(NOW, forms.consume("first", "cleanup-a"));
        assertEquals(NOW.plusSeconds(3), forms.consume("second", "cleanup-b"));
    }

    @Test
    void wrongEventConsumesTokenAndReplayFails() {
        RegistrationTokens forms = new RegistrationTokens();
        forms.issue("first", "cleanup-a", NOW);
        assertNull(forms.consume("first", "cleanup-b"));
        assertNull(forms.consume("first", "cleanup-a"));
        assertNull(forms.consume(null, "cleanup-a"));
    }

    @Test
    void oldAndExcessFormsAreEvicted() {
        RegistrationTokens forms = new RegistrationTokens();
        forms.issue("old", "cleanup", NOW.minusSeconds(7300));
        for (int i = 0; i < 9; i++) {
            forms.issue("form-" + i, "cleanup", NOW);
        }
        assertNull(forms.consume("old", "cleanup"));
        assertNull(forms.consume("form-0", "cleanup"));
        assertEquals(NOW, forms.consume("form-8", "cleanup"));
    }
}
