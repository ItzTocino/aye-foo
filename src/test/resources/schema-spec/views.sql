CREATE VIEW v_event_capacity AS
SELECT e.id,
       e.slug,
       e.title,
       e.capacity,
       e.seats_taken,
       GREATEST(e.capacity - e.seats_taken, 0) AS seats_left,
       (SELECT COUNT(*) FROM event_registrations r
         WHERE r.event_id = e.id AND r.status = 'waitlisted') AS waitlist_count
FROM events e;

CREATE VIEW v_donation_totals AS
SELECT currency,
       status,
       COUNT(*)                       AS gift_count,
       COALESCE(SUM(amount_cents), 0) AS total_cents
FROM donations
GROUP BY currency, status;

CREATE VIEW v_impact_summary AS
SELECT m.metric_key,
       m.label,
       m.unit,
       m.category,
       m.is_featured,
       m.display_order,
       CASE m.aggregation
         WHEN 'sum'    THEN COALESCE(SUM(e.value), 0)
         ELSE COALESCE((SELECT e2.value FROM impact_entries e2
                         WHERE e2.metric_id = m.id
                         ORDER BY e2.occurred_on DESC, e2.id DESC LIMIT 1), 0)
       END                       AS value,
       MAX(e.occurred_on)        AS last_fact_on
FROM impact_metrics m
LEFT JOIN impact_entries e ON e.metric_id = m.id
GROUP BY m.id, m.metric_key, m.label, m.unit, m.category,
         m.is_featured, m.display_order, m.aggregation;

CREATE VIEW v_partner_directory AS
SELECT id, name, slug, type, logo_path, website_url, blurb
FROM partners
WHERE is_active = 1
ORDER BY display_order, name;
