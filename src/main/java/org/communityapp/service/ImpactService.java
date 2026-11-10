package org.communityapp.service;

import java.util.List;
import org.communityapp.dao.ImpactDao;

/** Provides public impact facts and the approved featured subset. */
public final class ImpactService {
    private final ImpactDao dao;

    public ImpactService(ImpactDao dao) {
        this.dao = dao;
    }

    public List<ImpactDao.Metric> metrics() throws java.sql.SQLException {
        return dao.metrics();
    }

    public List<ImpactDao.Metric> featured(List<ImpactDao.Metric> metrics) {
        return metrics.stream().filter(ImpactDao.Metric::featured).limit(3).toList();
    }
}
