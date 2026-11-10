<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Chesapeake Bay initiatives"
             description="Shoreline and river cleanups, conservation, and education across the Chesapeake watershed."
             activeNav="initiatives">
  <section class="c-intro">
    <div class="c-container">
      <h1>Work along the watershed.</h1>
      <p class="c-lede">Browse approved project descriptions, local activity areas, and confirmed partner profiles.</p>
      <p class="c-content-note">Project descriptions are pending review by the SaveOurWorld team.</p>
      <c:if test="${contentUnavailable}">
        <p role="status">Initiative and partner information is temporarily unavailable. Please check back soon.</p>
      </c:if>
      <c:if test="${not contentUnavailable}">
        <c:if test="${empty initiatives}">
          <p>Published initiatives will appear here when their details are approved.</p>
        </c:if>
        <c:if test="${not empty initiatives}">
          <div class="c-editorial-list c-editorial-list--split">
          <c:forEach var="initiative" items="${initiatives}">
            <article><h2><c:out value="${initiative.title}" /></h2><div>
              <c:if test="${not empty initiative.location}"><p>Activity area: <c:out value="${initiative.location}" /></p></c:if>
              <p><c:out value="${initiative.summary}" /></p>
              <c:if test="${not empty initiative.body}"><p><c:out value="${initiative.body}" /></p></c:if></div>
            </article>
          </c:forEach>
          </div>
        </c:if>
      </c:if>
    </div>
  </section>
  <section id="partners" class="c-page-section c-section--sand" aria-labelledby="partners-title">
    <div class="c-container">
      <h2 id="partners-title">Partners</h2>
      <p>Partner profiles appear only after their details are confirmed for publication.</p>
      <c:if test="${not contentUnavailable}">
        <c:if test="${not empty partners}">
          <div class="c-editorial-list c-editorial-list--split">
          <c:forEach var="partner" items="${partners}">
            <article><h3><c:out value="${partner.name}" /></h3><div>
              <p><c:out value="${partner.blurb}" /></p>
              <c:if test="${not empty partner.websiteUrl}">
                <a href="<c:out value='${partner.websiteUrl}' />" rel="noopener noreferrer">Visit <c:out value="${partner.name}" /> website</a>
              </c:if></div>
            </article>
          </c:forEach>
          </div>
        </c:if>
        <c:if test="${empty partners}">
          <p>No partner profiles have been published yet.</p>
        </c:if>
      </c:if>
    </div>
  </section>
</layout:page>
