package org.communityapp.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.communityapp.dao.HealthDao;
import org.communityapp.dao.ContentDao;
import org.communityapp.dao.EventDao;
import org.communityapp.dao.RegistrationDao;
import org.communityapp.dao.DonationDao;
import org.communityapp.dao.ImpactDao;
import org.communityapp.service.HealthService;
import org.communityapp.service.ContentService;
import org.communityapp.service.EventService;
import org.communityapp.service.DonationService;
import org.communityapp.service.ImpactService;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composition root: builds the pool and the services once, in dependency order.
 */
@WebListener
public final class Wiring implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(Wiring.class);

    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig config = AppConfig.load();
        dataSource = DataSourceFactory.create(config);
        ServletContext context = event.getServletContext();
        context.setAttribute(HealthService.class.getName(), new HealthService(new HealthDao(dataSource)));
        context.setAttribute(ContentService.class.getName(), new ContentService(new ContentDao(dataSource)));
        context.setAttribute(EventService.class.getName(), new EventService(new EventDao(dataSource), Clock.systemUTC()));
        context.setAttribute(RegistrationDao.class.getName(), new RegistrationDao(dataSource));
        context.setAttribute(DonationService.class.getName(), new DonationService(new DonationDao(dataSource)));
        context.setAttribute(ImpactService.class.getName(), new ImpactService(new ImpactDao(dataSource)));
        // JDBC URLs can carry credentials in query parameters.
        log.info("data source initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
