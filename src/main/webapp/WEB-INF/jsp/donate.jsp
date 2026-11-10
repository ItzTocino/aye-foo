<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Donate"
             description="Support shoreline and river cleanups around the Chesapeake Bay."
             activeNav="donate">
  <section class="c-intro">
    <div class="c-container">
      <h1>Giving is not open yet.</h1>
      <p class="c-lede">The project is preparing its card payment account. This page does not accept or charge payments.</p>
      <section class="c-donation-ledger" aria-labelledby="donation-goal-title">
        <div><h2 id="donation-goal-title">Project demo ledger</h2><p>Demo goal: $2,500 USD. This is a record of test or entered gifts, not a live fundraiser.</p></div>
        <div class="c-donation-ledger__progress">
        <c:choose>
          <c:when test="${donationProgressUnavailable}">
            <p class="c-status" role="status">Recorded donation totals are temporarily unavailable. Please check back later.</p>
          </c:when>
          <c:when test="${not empty donationProgress}">
            <p>Recorded successful USD donations: <strong><c:out value="${donationProgress.amount}" /></strong>
              of <c:out value="${donationProgress.goal}" /> from
              <c:out value="${donationProgress.count}" /> recorded gifts.</p>
            <progress aria-label="Recorded donation goal progress" max="100" value="${donationProgress.percent}">
              <c:out value="${donationProgress.percent}" />%
            </progress>
            <p>This is a ledger summary for the project demo. Records may represent test payments;
              this figure does not verify funds received by the organization.</p>
          </c:when>
        </c:choose>
        </div>
      </section>
    </div>
  </section>
</layout:page>
