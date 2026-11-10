# SaveOurWorld

A Java web project for a Chesapeake Bay cleanup organization. It includes public information pages, cleanup events, event registration, and recorded donation and impact data.

## Run locally

Install JDK 17, Maven 3.9, and MySQL 8. Create a local `saveourworld` database and user, then set `DB_URL`, `DB_USER`, and `DB_PASSWORD` for your MySQL setup. The defaults are in `src/main/resources/app.properties`.

```powershell
mvn compile exec:java -Dexec.mainClass=org.communityapp.db.Migrate
mvn verify
mvn -Pdev exec:exec
```

Open http://localhost:8080. Run `mvn verify` again after source changes to rebuild the web application. `/healthz` reports whether the database is reachable.

## Project status

This is a work in progress. Online payments and confirmation email delivery are not enabled. Public content and partner information need approval before launch. When database content is unavailable, pages display an unavailable or empty state.

The application uses Java, Jakarta Servlet and JSP, JDBC, MySQL, Maven, and Tomcat. Application code is under `src/main`, and tests are under `src/test`.
