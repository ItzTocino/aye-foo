<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Cleanups on the Chesapeake Bay" description="Find Chesapeake Bay cleanup opportunities, published project records, and ways to take part." activeNav="home">
  <section class="c-hero c-hero--editorial">
    <div class="c-container c-hero__grid">
      <div>
        <h1>Care for the water we share.</h1>
        <p class="c-lede">Shoreline and river cleanups in the Chesapeake Bay watershed. Find a published event, learn about the work, or follow recorded progress.</p>
        <div class="c-actions">
          <a class="c-button c-button--primary" href="${pageContext.request.contextPath}/get-involved">Find a cleanup</a>
          <a class="c-button c-button--secondary" href="${pageContext.request.contextPath}/initiatives">See the work</a>
        </div>
      </div>
      <aside class="c-hero__aside" aria-label="Publication status">
        <p class="c-hero__place">Chesapeake Bay watershed</p>
        <p class="c-content-note">Site content is a working draft awaiting approval from the organization. Event and partner details appear only when published.</p>
      </aside>
    </div>
  </section>
  <section class="c-page-section" aria-labelledby="work-title">
    <div class="c-container">
      <div class="c-section-head"><h2 id="work-title">Start with a shoreline.</h2><p>Rivers and creeks connect neighborhoods to the Bay. Find local cleanup opportunities and learn about the conservation projects.</p></div>
      <div class="c-editorial-list c-editorial-list--split">
        <article><h3>Find a published cleanup</h3><div><p>See confirmed dates, meeting locations, and current availability before planning a visit.</p><a href="${pageContext.request.contextPath}/get-involved#calendar">Upcoming events</a></div></article>
        <article><h3>Explore local initiatives</h3><div><p>Read the project descriptions and their publication status, including conservation and community education work.</p><a href="${pageContext.request.contextPath}/initiatives">Initiatives and partners</a></div></article>
        <article><h3>Check the recorded results</h3><div><p>Impact facts are dated ledger entries. Missing facts remain unpublished and recorded totals are not independent verification.</p><a href="${pageContext.request.contextPath}/impact">Impact dashboard</a></div></article>
      </div>
    </div>
  </section>
  <section class="c-page-section c-section--sand" aria-labelledby="home-impact-title">
    <div class="c-container">
      <div class="c-section-head"><h2 id="home-impact-title">What the project records show</h2><p>These figures come from the project ledger and have not been independently verified.</p></div>
      <c:choose>
        <c:when test="${impactUnavailable}"><p class="c-status" role="status">Impact figures are temporarily unavailable.</p></c:when>
        <c:when test="${empty featuredImpact}"><p class="c-status">No featured impact facts have been published yet.</p></c:when>
        <c:otherwise><ul class="c-data-list"><c:forEach var="metric" items="${featuredImpact}"><li><h3><c:out value="${metric.label}" /></h3><p><strong><c:out value="${metric.value}" /></strong> <c:out value="${metric.unit}" /></p><p>Latest record: <c:out value="${metric.lastFactOn}" /></p></li></c:forEach></ul></c:otherwise>
      </c:choose>
      <a class="c-simple-cta" href="${pageContext.request.contextPath}/impact">View all recorded impact &rarr;</a>
    </div>
  </section>
  <section class="c-page-section" aria-labelledby="home-partners-title">
    <div class="c-container c-prose-grid"><div><h2 id="home-partners-title">Who is working alongside the project?</h2><p>Partner profiles are displayed only after their details have been confirmed.</p>
      <c:choose><c:when test="${partnersUnavailable}"><p class="c-status" role="status">Partner listings are temporarily unavailable.</p></c:when><c:when test="${empty partners}"><p class="c-status">No partner profiles have been confirmed for publication yet.</p></c:when><c:otherwise><ul class="c-editorial-list c-editorial-list--names"><c:forEach var="partner" items="${partners}"><li><strong><c:out value="${partner.name}" /></strong></li></c:forEach></ul></c:otherwise></c:choose>
      <a href="${pageContext.request.contextPath}/initiatives#partners">Partner profiles</a></div>
      <aside><h2>Take part</h2><p>Look through published events before making travel plans. Online donations are not open yet.</p><p><a href="${pageContext.request.contextPath}/get-involved">Ways to participate</a></p><p><a href="${pageContext.request.contextPath}/donate">Donation status</a></p></aside>
    </div>
  </section>
</layout:page>
