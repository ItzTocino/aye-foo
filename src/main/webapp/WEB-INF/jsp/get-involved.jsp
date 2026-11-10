<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Get involved"
             description="Ways to help SaveOurWorld: join a cleanup, give, or spread the word."
             activeNav="get-involved">
  <section class="c-intro">
    <div class="c-container">
      <h1>Find a cleanup near you.</h1>
      <p class="c-lede">Find published cleanup dates and the information you need before attending.</p>
      <p class="c-content-note">Published events can offer registration through their detail page. General volunteer sign-up is not open yet.</p>
      <div class="c-actions">
        <a class="c-button c-button--primary" href="#calendar">Check cleanup dates</a>
        <a class="c-button c-button--secondary" href="${pageContext.request.contextPath}/initiatives">Explore initiatives</a>
      </div>
    </div>
  </section>
  <section class="c-page-section c-section--sand" id="calendar" aria-labelledby="calendar-title">
    <div class="c-container">
      <h2 id="calendar-title">Upcoming cleanups</h2>
      <c:choose>
        <c:when test="${calendarUnavailable}">
          <p role="status">Upcoming cleanup information is temporarily unavailable. Please check back soon.</p>
        </c:when>
        <c:when test="${empty upcomingEvents}">
          <p>No cleanup dates are published on this page yet. Dates and meeting points will appear here once confirmed and published.</p>
        </c:when>
        <c:otherwise>
          <div class="c-editorial-list c-editorial-list--split" aria-label="Published upcoming cleanups">
            <c:forEach var="event" items="${upcomingEvents}">
              <article>
                <h3><c:out value="${event.title}" /></h3><div>
                <p><strong><c:out value="${event.when}" /></strong></p>
                <c:if test="${not empty event.place}"><p>Meeting point: <c:out value="${event.place}" /></p></c:if>
                <c:if test="${not empty event.summary}"><p><c:out value="${event.summary}" /></p></c:if>
                <p><c:out value="${event.availability}" /></p>
                <c:if test="${not empty event.slug}"><p><a href="${pageContext.request.contextPath}/events/${event.slug}">View details for <c:out value="${event.title}" /></a></p></c:if></div>
              </article>
            </c:forEach>
          </div>
          <p>Open a published event's detail page to check its registration or waitlist options. Confirm the meeting point before traveling.</p>
        </c:otherwise>
      </c:choose>
      <p>General volunteer sign-up is also being prepared. Please do not travel to a cleanup without a confirmed event listing.</p>
    </div>
  </section>
  <section class="c-page-section" aria-labelledby="ways-title">
    <div class="c-container">
      <h2 id="ways-title">Other ways to take part</h2>
      <div class="c-editorial-list c-editorial-list--simple">
        <article>
          <h3>Show up</h3>
          <p>Learn about local cleanup and conservation efforts while event registration is being set up.</p>
          <p><a href="${pageContext.request.contextPath}/initiatives">See the initiatives</a></p>
        </article>
        <article>
          <h3>Give</h3>
          <p>Online donations are not open yet. Read the donation status before making plans to contribute.</p>
          <p><a href="${pageContext.request.contextPath}/donate">Donation information</a></p>
        </article>
        <article>
          <h3>Spread the word</h3>
          <p>Share the public project pages with someone interested in Chesapeake Bay waterways.</p>
          <p><a href="${pageContext.request.contextPath}/about">Read about the project</a></p>
        </article>
      </div>
    </div>
  </section>
  <section class="c-page-section c-section--sand" aria-labelledby="faq-title">
    <div class="c-container">
      <div class="c-section-head"><h2 id="faq-title">Plan for a cleanup</h2><p>Check published event details for location, eligibility and weather updates.</p></div>
      <dl class="c-faq">
        <div><dt>What should I bring?</dt><dd>What to bring and where to meet will be provided with each confirmed event.</dd></div>
        <div><dt>Can younger volunteers attend?</dt><dd>Age and guardian requirements have not been confirmed. Check published event details before planning to attend.</dd></div>
        <div><dt>What happens if the weather changes?</dt><dd>Weather and cancellation instructions will be included with confirmed event details.</dd></div>
        <div><dt>Can this count toward court-ordered service hours?</dt><dd>Court-ordered service hours are not supported in the initial version.</dd></div>
      </dl>
      <p>Have another question? <a href="${pageContext.request.contextPath}/contact">See current contact information</a>.</p>
    </div>
  </section>
</layout:page>
