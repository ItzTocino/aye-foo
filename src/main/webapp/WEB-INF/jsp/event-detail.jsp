<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Cleanup event details"
             description="Published cleanup event details and current availability."
             activeNav="get-involved">
  <section class="c-intro">
    <div class="c-container">
      <p><a href="${pageContext.request.contextPath}/get-involved#calendar">&larr; All upcoming cleanups</a></p>
      <h1><c:out value="${event.title}" /></h1>
      <c:if test="${not empty event.summary}"><p class="c-lede"><c:out value="${event.summary}" /></p></c:if>
      <div class="c-prose-grid"><div><h2>When and where</h2>
      <p><strong>Starts:</strong> <c:out value="${event.start}" /></p>
      <c:if test="${not empty event.end}"><p><strong>Ends:</strong> <c:out value="${event.end}" /></p></c:if>
      <c:choose>
        <c:when test="${not empty event.location}"><p><strong>Meeting point:</strong> <c:out value="${event.location}" /></p></c:when>
        <c:otherwise><p>Meeting point has not been published yet. Check for updates before traveling.</p></c:otherwise>
      </c:choose>
      <c:if test="${not empty event.mapUrl}"><p><a href="<c:out value='${event.mapUrl}' />" rel="noopener noreferrer" target="_blank">View meeting point on map (opens a new tab)</a></p></c:if>
      <h2>About this cleanup</h2>
      <c:choose>
        <c:when test="${not empty event.description}"><p class="c-event-description"><c:out value="${event.description}" /></p></c:when>
        <c:otherwise><p>Additional instructions, including what to bring, have not been published yet.</p></c:otherwise>
      </c:choose>
      </div><aside><h2>Availability</h2>
      <p><c:out value="${event.seats}" />.</p>
      <c:if test="${event.waitlist != '0'}"><p><c:out value="${event.waitlist}" /> people currently on the waitlist.</p></c:if>
      <c:choose>
        <c:when test="${registrationStatus == 'registered'}"><p role="status">Your cleanup spot is reserved. Confirmation email is queued.</p></c:when>
        <c:when test="${registrationStatus == 'waitlisted'}"><p role="status">You have joined the waitlist. This is not a reserved spot; confirmation email is queued.</p></c:when>
        <c:when test="${registrationStatus == 'duplicate'}"><p role="status">This email already has a signup for this cleanup. Contact us if you need help.</p></c:when>
      </c:choose>
      </aside></div>
    </div>
  </section>
  <section class="c-page-section c-section--sand" aria-labelledby="signup-title">
    <div class="c-container">
      <h2 id="signup-title">Sign up for this cleanup</h2>
      <p>When all spaces are reserved, new signups join the waitlist. We will not treat a waitlist spot as a reservation.</p>
      <form class="c-form" method="post" action="${pageContext.request.contextPath}/events/${eventSlug}">
        <input type="hidden" name="csrf" value="<c:out value='${registrationToken}' />">
        <div hidden aria-hidden="true"><label>Website <input name="website" tabindex="-1" autocomplete="off"></label></div>
        <div class="c-form-fields"><p><label for="firstName">First name (required)</label><br><input id="firstName" name="firstName" required maxlength="80" autocomplete="given-name"></p>
        <p><label for="lastName">Last name (required)</label><br><input id="lastName" name="lastName" required maxlength="80" autocomplete="family-name"></p>
        <p><label for="email">Email (required)</label><br><input id="email" name="email" type="email" required maxlength="254" autocomplete="email"></p>
        <p><label for="phone">Phone (optional)</label><br><input id="phone" name="phone" type="tel" maxlength="32" autocomplete="tel"></p>
        <p><label for="city">City (optional)</label><br><input id="city" name="city" maxlength="100" autocomplete="address-level2"></p>
        <p><label for="state">State, two-letter code (optional)</label><br><input id="state" name="state" maxlength="2" pattern="[A-Za-z]{2}" autocomplete="address-level1"></p>
        </div><p><label><input name="consent" type="checkbox" value="yes" required> I agree to be contacted about this cleanup and my registration.</label></p>
        <p><button class="c-button c-button--primary" type="submit">Submit registration or join waitlist</button></p>
      </form>
      <p><a href="${pageContext.request.contextPath}/contact">Questions about attending?</a></p>
    </div>
  </section>
</layout:page>
