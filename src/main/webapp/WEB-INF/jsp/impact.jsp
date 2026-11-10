<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<layout:page pageTitle="Impact dashboard"
             description="Recorded Chesapeake Bay cleanup and community impact.">
  <section class="c-intro">
    <div class="c-container">
      <h1>What the ledger records.</h1>
      <p class="c-lede">Impact figures come from dated records entered in the project ledger. Figures will appear here when records are available; they have not been independently verified.</p>
      <c:choose>
        <c:when test="${impactUnavailable}">
          <p role="status">Impact records are temporarily unavailable. Please try again later.</p>
        </c:when>
        <c:when test="${empty impactMetrics}">
          <p>No impact facts have been published yet.</p>
        </c:when>
        <c:otherwise>
          <c:forEach var="category" items="${impactCategories}">
            <section aria-label="${category} impact">
              <h2><c:out value="${category == 'cleanup' ? 'Cleanup' : category == 'conservation' ? 'Conservation' : 'Community'}" /></h2>
              <ul class="c-data-list">
                <c:forEach var="metric" items="${impactMetrics}">
                  <c:if test="${metric.category == category}">
                    <li>
                      <h3><c:out value="${metric.label}" /></h3>
                      <p><strong><c:out value="${metric.value}" /></strong> <c:out value="${metric.unit}" /></p>
                      <p>Latest record: <c:out value="${metric.lastFactOn}" /></p>
                    </li>
                  </c:if>
                </c:forEach>
              </ul>
            </section>
          </c:forEach>
        </c:otherwise>
      </c:choose>
      <p>Data is recorded per activity and summarized by the metric definitions in the project database. No estimates are substituted for missing records.</p>
      <p><a href="${pageContext.request.contextPath}/">Back to Home</a></p>
    </div>
  </section>
</layout:page>
