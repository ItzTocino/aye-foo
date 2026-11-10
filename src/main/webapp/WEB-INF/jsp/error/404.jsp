<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="Page not found"
             description="That page does not exist.">
  <section class="c-intro c-intro--error">
    <div class="c-container">
      <h1>Page not found.</h1>
      <p>The address may have changed, or the link may be wrong.</p>
      <ul class="c-link-list">
        <li><a href="${pageContext.request.contextPath}/">Go to the home page</a></li>
        <li><a href="${pageContext.request.contextPath}/get-involved">Get involved</a></li>
      </ul>
    </div>
  </section>
</layout:page>
