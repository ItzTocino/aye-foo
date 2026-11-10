<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="That link is not valid"
             description="The link you followed is not valid.">
  <section class="c-intro c-intro--error">
    <div class="c-container">
      <h1>That link is not valid.</h1>
      <p>The link may have expired, or it may have been copied incompletely.</p>
      <ul class="c-link-list">
        <li><a href="${pageContext.request.contextPath}/">Go to the home page</a></li>
        <li><a href="${pageContext.request.contextPath}/get-involved">Get involved</a></li>
      </ul>
    </div>
  </section>
</layout:page>
