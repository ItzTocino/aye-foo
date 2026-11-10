package org.communityapp.service;

import org.communityapp.dao.HealthDao;

/**
 * Reports whether the site can reach its database.
 */
public final class HealthService {

    private final HealthDao healthDao;

    public HealthService(HealthDao healthDao) {
        this.healthDao = healthDao;
    }

    public boolean isReady() {
        return healthDao.ping();
    }
}
