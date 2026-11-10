<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="Something went wrong"
             description="An unexpected error stopped this page.">
  <section class="c-intro c-intro--error">
    <div class="c-container">
      <h1>Something went wrong.</h1>
      <p>The error was logged. Reference: ${empty requestScope.correlationId ? 'unavailable' : requestScope.correlationId}</p>
      <ul class="c-link-list">
        <li><a href="${pageContext.request.contextPath}/">Go to the home page</a></li>
        <li><a href="${pageContext.request.contextPath}/get-involved">Get involved</a></li>
      </ul>
    </div>
  </section>
</layout:page>
