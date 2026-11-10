<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="Temporarily unavailable"
             description="Some public information is temporarily unavailable.">
  <section class="c-intro c-intro--error">
    <div class="c-container">
      <h1>Temporarily unavailable.</h1>
      <p>Cleanup details are temporarily unavailable. Please try again later. You can still browse the other public pages.</p>
      <ul class="c-link-list">
        <li><a href="${pageContext.request.contextPath}/get-involved">Return to upcoming cleanups</a></li>
        <li><a href="${pageContext.request.contextPath}/">Go to the home page</a></li>
      </ul>
    </div>
  </section>
</layout:page>
