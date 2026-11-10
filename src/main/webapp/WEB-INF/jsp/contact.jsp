<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="Contact"
             description="Questions, partnerships, and press for SaveOurWorld."
             activeNav="contact">
  <section class="c-intro">
    <div class="c-container">
      <h1>Contact information is coming.</h1>
      <p class="c-lede">The public inbox and contact form have not yet been confirmed.</p>
      <p class="c-content-note">Working draft: the public contact address and form have not yet been confirmed or opened.</p>
      <h2>How to reach us</h2>
      <p>The contact form and public inbox details will appear here when they are ready. There is no active message submission on this page yet.</p>
      <p>A response-time estimate is not yet available.</p>
    </div>
  </section>
  <section class="c-page-section c-section--sand" aria-labelledby="contact-topics-title">
    <div class="c-container">
      <h2 id="contact-topics-title">What would you like to discuss?</h2>
      <div class="c-editorial-list c-editorial-list--simple">
        <article>
          <h3>Cleanup participation</h3>
          <p>Looking for an event or general volunteering? See the current schedule status and participation options.</p>
          <p><a href="${pageContext.request.contextPath}/get-involved#calendar">Check upcoming cleanups</a></p>
        </article>
        <article>
          <h3>Partnerships</h3>
          <p>Explore the partner directory and initiative areas before a contact channel is published.</p>
          <p><a href="${pageContext.request.contextPath}/initiatives#partners">View partner information</a></p>
        </article>
        <article>
          <h3>Press and general questions</h3>
          <p>A public inbox for questions and press requests is still pending confirmation.</p>
          <p><a href="${pageContext.request.contextPath}/about">About SaveOurWorld</a></p>
        </article>
      </div>
    </div>
  </section>
</layout:page>
