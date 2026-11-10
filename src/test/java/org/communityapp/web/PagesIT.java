package org.communityapp.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Boots the real WAR in Tomcat and checks that every public page renders.
 */
class PagesIT {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static Tomcat tomcat;
    private static String baseUrl;

    @BeforeAll
    static void startTomcat() throws Exception {
        Path webapp = Path.of(System.getProperty("webapp.dir")).toAbsolutePath();
        assertTrue(Files.isDirectory(webapp), "exploded WAR is missing: " + webapp);

        tomcat = EmbeddedTomcat.start(webapp, 0);
        baseUrl = "http://localhost:" + EmbeddedTomcat.port(tomcat);
    }

    @AfterAll
    static void stopTomcat() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/about", "/initiatives", "/get-involved", "/contact"})
    void page_whenRequested_returnsRenderedHtml(String path) throws Exception {
        HttpResponse<String> response = get(path);

        assertEquals(200, response.statusCode(), path);
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("text/html"), path);
        assertTrue(response.body().contains("</html>"), path);
        assertTrue(response.body().contains("href=\"#main\">Skip to content</a>"), path);
        assertTrue(response.body().contains("<main id=\"main\" tabindex=\"-1\">"), path);
        assertTrue(response.body().contains("aria-label=\"Main\""), path);
        assertTrue(response.body().contains("aria-label=\"Footer\""), path);
    }

    @Test
    void navigationStyles_keepKeyboardTargetsLarge() throws Exception {
        HttpResponse<String> styles = get("/css/components.css");
        assertEquals(200, styles.statusCode());
        assertTrue(styles.body().contains(".c-nav__link {"));
        assertTrue(styles.body().contains(".c-footer__nav a,"));
        assertTrue(styles.body().contains("min-height: 44px;"));
    }

    @Test
    void donate_whenRequested_returnsRenderedHtml() throws Exception {
        HttpResponse<String> response = get("/donate");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<h1>Giving is not open yet.</h1>"));
        assertTrue(response.body().contains("Demo goal: $2,500 USD"));
        assertTrue(response.body().contains("This page does not accept or charge payments."));
    }

    @Test
    void impact_whenRequested_hasAccessibleDashboardAndNavigation() throws Exception {
        HttpResponse<String> dashboard = get("/impact");
        assertEquals(200, dashboard.statusCode());
        assertTrue(dashboard.body().contains("Impact dashboard"));
        assertTrue(dashboard.body().contains("Back to Home"));
        assertTrue(dashboard.body().contains("c-footer"));
        HttpResponse<String> home = get("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("/impact"));
    }

    @Test
    void impact_whenDatabaseDegradedDoesNotInventFacts() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> response = get("/impact");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Impact records are temporarily unavailable."));
        assertTrue(!response.body().contains("Latest recorded fact:"));
    }

    @Test
    void home_whenDatabaseConnectionFailsShowsBothUnavailableStates() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> home = get("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("Partner listings are temporarily unavailable"));
        assertTrue(home.body().contains("Impact figures are temporarily unavailable"));
    }

    @Test
    void donate_whenDatabaseDegradedNamesUnavailableTotalsWithoutMakingUpDonations() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> response = get("/donate");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Recorded donation totals are temporarily unavailable."));
        assertTrue(!response.body().contains("Recorded successful USD donations:"));
        assertTrue(response.body().contains("c-footer"));
    }

    @Test
    void page_whenUnknown_returns404InTheSiteChrome() throws Exception {
        HttpResponse<String> response = get("/no-such-page");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Page not found."));
        assertTrue(response.body().contains("c-footer"));
    }

    @Test
    void health_whenProbed_reportsJsonStatus() throws Exception {
        HttpResponse<String> response = get("/healthz");

        int status = response.statusCode();
        assertTrue(status == 200 || status == 503, "unexpected status " + status);
        assertEquals(status == 200 ? "{\"status\":\"ok\"}" : "{\"status\":\"degraded\"}", response.body());
    }

    @Test
    void page_whenRequested_carriesSecurityHeaders() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals("nosniff", response.headers().firstValue("x-content-type-options").orElse(""));
        assertTrue(response.headers().firstValue("content-security-policy")
                .orElse("").contains("frame-ancestors 'none'"));
        assertTrue(response.headers().firstValue("strict-transport-security").isEmpty(),
                "local HTTP must remain usable without HSTS");
    }

    @Test
    void contentPages_showDraftLabelsAndMetadata() throws Exception {
        HttpResponse<String> home = get("/");
        HttpResponse<String> about = get("/about");
        HttpResponse<String> initiatives = get("/initiatives");

        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("working draft"));
        assertTrue(home.body().contains("property=\"og:title\""));
        assertTrue(home.body().contains("id=\"home-partners-title\""));
        assertTrue(home.body().contains("/initiatives#partners"));
        assertTrue(home.body().contains("Partner listings are temporarily unavailable.")
                || home.body().contains("Partner profiles have not been confirmed")
                || home.body().contains("aria-label=\"Confirmed community partners\""));
        assertEquals(200, about.statusCode());
        assertTrue(about.body().contains("awaiting confirmation"));
        assertEquals(200, initiatives.statusCode());
        assertTrue(initiatives.body().contains("id=\"partners-title\""));
        assertTrue(initiatives.body().contains("id=\"partners\""));
        assertTrue(initiatives.body().contains("temporarily unavailable")
                || initiatives.body().contains("Published initiatives")
                || initiatives.body().contains("c-card-grid"));
    }

    @Test
    void editorialPages_showSectionHeadingsWithoutDecorativeLabels() throws Exception {
        HttpResponse<String> home = get("/");
        HttpResponse<String> involved = get("/get-involved");
        HttpResponse<String> initiatives = get("/initiatives");

        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("<h2 id=\"work-title\">Start with a shoreline.</h2>"));
        assertFalse(home.body().contains("01 /"));
        assertFalse(home.body().contains("02 /"));
        assertFalse(home.body().contains("03 /"));
        assertFalse(home.body().contains("c-row-label"));
        assertEquals(200, involved.statusCode());
        assertFalse(involved.body().contains("c-row-label"));
        assertFalse(involved.body().contains("<p>Before you go</p>"));
        assertEquals(200, initiatives.statusCode());
        assertFalse(initiatives.body().contains("c-row-label"));
    }

    @Test
    void participationPages_offerWorkingRoutesWithoutInventingAvailability() throws Exception {
        HttpResponse<String> involved = get("/get-involved");
        HttpResponse<String> contact = get("/contact");

        assertEquals(200, involved.statusCode());
        assertTrue(involved.body().contains("<h1>Find a cleanup near you.</h1>"));
        assertTrue(involved.body().contains("id=\"calendar\""));
        assertTrue(involved.body().contains("No cleanup dates are published on this page yet.")
                || involved.body().contains("Upcoming cleanup information is temporarily unavailable.")
                || involved.body().contains("Published upcoming cleanups"));
        assertTrue(involved.body().contains("Online donations are not open yet."));
        assertTrue(involved.body().contains("/donate\""));
        assertTrue(involved.body().contains("/contact\""));
        assertTrue(involved.body().contains("aria-current=\"page\""));

        assertEquals(200, contact.statusCode());
        assertTrue(contact.body().contains("<h1>Contact information is coming.</h1>"));
        assertTrue(contact.body().contains("no active message submission"));
        assertTrue(contact.body().contains("/get-involved#calendar"));
        assertTrue(contact.body().contains("/initiatives#partners"));
        assertTrue(contact.body().contains("public inbox"));
        assertTrue(get("/initiatives").body().contains("id=\"partners\""));
    }

    @Test
    void home_whenDatabaseDegraded_keepsPageAvailableAndNamesMissingPartners() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> home = get("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("Partner listings are temporarily unavailable."));
        assertTrue(home.body().contains("/initiatives#partners"));
    }

    @Test
    void calendar_whenDatabaseDegraded_keepsPageAvailableWithoutClaimingNoEvents() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> involved = get("/get-involved");
        assertEquals(200, involved.statusCode());
        assertTrue(involved.body().contains("Upcoming cleanup information is temporarily unavailable."));
        assertTrue(!involved.body().contains("No cleanup dates are published on this page yet."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/events", "/events/", "/events/bad/path", "/events/Bad", "/events/bad--slug"})
    void eventDetail_invalidPathReturns404(String path) throws Exception {
        HttpResponse<String> response = get(path);
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Page not found."));
    }

    @Test
    void eventDetail_whenDatabaseDegradedReturns503() throws Exception {
        Assumptions.assumeTrue(get("/healthz").statusCode() == 503,
                "requires an unavailable database");
        HttpResponse<String> response = get("/events/legitimate-cleanup");
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("Cleanup details are temporarily unavailable."));
        assertTrue(response.body().contains("/get-involved"));
        assertTrue(response.body().contains("c-footer"));
    }

    @Test
    void registration_withoutFormTokenRejectsPostBeforeDatabaseAccess() throws Exception {
        HttpRequest post = HttpRequest.newBuilder(URI.create(baseUrl + "/events/legitimate-cleanup"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "firstName=Ada&lastName=Lovelace&email=ada%40example.org&consent=yes"))
                .build();
        HttpResponse<String> response = CLIENT.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    void registration_rejectsMalformedSlugBeforeFormProcessing() throws Exception {
        HttpRequest post = HttpRequest.newBuilder(URI.create(baseUrl + "/events/Bad-Slug"))
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = CLIENT.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void crawlAssets_arePresent() throws Exception {
        assertEquals(200, get("/favicon.svg").statusCode());
        assertEquals(200, get("/robots.txt").statusCode());
        // The production sitemap intentionally requires a canonical public origin.
        assertEquals(503, get("/sitemap.xml").statusCode());
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
