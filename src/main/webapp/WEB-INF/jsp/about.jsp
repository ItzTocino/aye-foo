<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags" %>
<layout:page pageTitle="About us"
             description="Who SaveOurWorld is and why we work on the Chesapeake Bay."
             activeNav="about">
  <section class="c-intro">
    <div class="c-container">
      <h1>The work begins close to home.</h1>
      <p class="c-lede">SaveOurWorld is a volunteer-run cleanup group working on the Chesapeake Bay and its rivers.</p>
      <p class="c-content-note">Draft organization description - awaiting confirmation from Lizbeth.</p>
      <div class="c-prose-grid"><div><h2>Why the Chesapeake Bay?</h2>
      <p>Waterways throughout the Chesapeake watershed connect communities to the Bay. Trash and pollution do not stop at town boundaries, which is why local cleanup efforts matter.</p>
      <h2>Our approach</h2>
      <p>We aim to make it easier for neighbors to find ways to care for local waterways: showing up for cleanups, learning about conservation, and connecting with others who share that goal.</p>
      </div><aside><h2>Get involved</h2><p>Find the current events and participation status before making plans.</p><p><a href="${pageContext.request.contextPath}/get-involved">Participation information</a></p></aside></div><p>Interested in helping shape the next cleanup? <a href="${pageContext.request.contextPath}/contact">Get in touch</a>.</p>
    </div>
  </section>
</layout:page>
