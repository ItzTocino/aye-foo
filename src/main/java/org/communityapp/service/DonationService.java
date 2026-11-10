package org.communityapp.service;

import java.text.NumberFormat;
import java.util.Locale;
import org.communityapp.dao.DonationDao;

/** Read-only progress of recorded successful USD gifts. */
public final class DonationService {
    public static final long GOAL_CENTS = 250_000;
    private final DonationDao dao;

    public DonationService(DonationDao dao) {
        this.dao = dao;
    }

    public Progress progress() throws java.sql.SQLException {
        DonationDao.Totals totals = dao.succeededUsd();
        NumberFormat usd = NumberFormat.getCurrencyInstance(Locale.US);
        long percent = totals.cents() >= GOAL_CENTS
                ? 100 : totals.cents() * 100 / GOAL_CENTS;
        return new Progress(usd.format(totals.cents() / 100.0), usd.format(GOAL_CENTS / 100.0),
                totals.count(), percent);
    }

    public record Progress(String amount, String goal, long count, long percent) {
        // JSP EL uses JavaBean properties, not record component accessors.
        public String getAmount() { return amount; }
        public String getGoal() { return goal; }
        public long getCount() { return count; }
        public long getPercent() { return percent; }
    }
}
