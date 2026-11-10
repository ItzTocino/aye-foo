<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="pageTitle" required="true" %>
<%@ attribute name="description" required="true" %>
<%@ attribute name="activeNav" required="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${pageTitle} - SaveOurWorld</title>
  <meta name="description" content="${description}">
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="SaveOurWorld">
  <meta property="og:title" content="${pageTitle} - SaveOurWorld">
  <meta property="og:description" content="${description}">
  <meta name="theme-color" content="#F6F2E9">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/tokens.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/base.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components.css">
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/favicon.svg">
</head>
<body>
<a class="c-skip-link" href="#main">Skip to content</a>

<header class="c-header">
  <div class="c-container c-header__inner">
    <a class="c-header__brand" href="${pageContext.request.contextPath}/">SaveOurWorld</a>
    <nav class="c-nav" aria-label="Main">
      <a class="c-nav__link ${activeNav == 'home' ? 'is-active' : ''}"
         href="${pageContext.request.contextPath}/"
         aria-current="${activeNav == 'home' ? 'page' : 'false'}">Home</a>
      <a class="c-nav__link ${activeNav == 'about' ? 'is-active' : ''}"
         href="${pageContext.request.contextPath}/about"
         aria-current="${activeNav == 'about' ? 'page' : 'false'}">About us</a>
      <a class="c-nav__link ${activeNav == 'initiatives' ? 'is-active' : ''}"
         href="${pageContext.request.contextPath}/initiatives"
         aria-current="${activeNav == 'initiatives' ? 'page' : 'false'}">Initiatives</a>
      <a class="c-nav__link ${activeNav == 'get-involved' ? 'is-active' : ''}"
         href="${pageContext.request.contextPath}/get-involved"
         aria-current="${activeNav == 'get-involved' ? 'page' : 'false'}">Get involved</a>
      <a class="c-nav__link ${activeNav == 'contact' ? 'is-active' : ''}"
         href="${pageContext.request.contextPath}/contact"
         aria-current="${activeNav == 'contact' ? 'page' : 'false'}">Contact</a>
    </nav>
    <a class="c-button c-button--primary" href="${pageContext.request.contextPath}/donate" aria-current="${activeNav == 'donate' ? 'page' : 'false'}">Donate</a>
  </div>
</header>

<main id="main" tabindex="-1">
  <jsp:doBody/>
</main>

<footer class="c-footer">
  <div class="c-container c-footer__inner">
    <div class="c-footer__brand">
      <p class="c-footer__name">SaveOurWorld</p>
      <p>Shoreline and river cleanups around the Chesapeake Bay.</p>
    </div>
    <nav class="c-footer__nav" aria-label="Footer">
      <a href="${pageContext.request.contextPath}/about">About us</a>
      <a href="${pageContext.request.contextPath}/initiatives">Initiatives</a>
      <a href="${pageContext.request.contextPath}/get-involved">Get involved</a>
      <a href="${pageContext.request.contextPath}/contact">Contact</a>
      <a href="${pageContext.request.contextPath}/donate">Donate</a>
      <a href="${pageContext.request.contextPath}/impact">Impact dashboard</a>
    </nav>
    <div class="c-footer__contact">
      <p>Questions, partnerships, press</p>
      <a href="${pageContext.request.contextPath}/contact">Contact us</a>
    </div>
  </div>
  <div class="c-container c-footer__legal">
    <p>SaveOurWorld · Chesapeake Bay</p>
  </div>
</footer>

</body>
</html>
