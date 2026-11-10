CREATE DATABASE IF NOT EXISTS saveourworld
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE saveourworld;

-- ─── Operational ──────────────────────────────────────────────────────────
-- The migrator creates this table before the first migration, then records
-- (version, sha256) for every file it applies. A changed checksum fails startup.
CREATE TABLE IF NOT EXISTS schema_migrations (
  version    VARCHAR(20)  NOT NULL,
  checksum   CHAR(64)     NOT NULL,
  applied_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB;

CREATE TABLE app_meta (
  meta_key   VARCHAR(60)  NOT NULL,
  meta_value VARCHAR(255) NOT NULL,
  updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (meta_key)
) ENGINE=InnoDB;
-- cache_version is bumped in the same transaction as any public content write.

CREATE TABLE rate_limits (
  bucket_key   VARCHAR(190)    NOT NULL,   -- 'login:ip:1.2.3.4:2026110614'
  window_start DATETIME        NOT NULL,
  count        INT UNSIGNED    NOT NULL DEFAULT 0,
  PRIMARY KEY (bucket_key, window_start)
) ENGINE=InnoDB;
-- INSERT ... ON DUPLICATE KEY UPDATE count = count + 1; prune rows older than 24 h.

CREATE TABLE webhook_events (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  processor      VARCHAR(32)     NOT NULL DEFAULT 'stripe',
  event_id       VARCHAR(128)    NOT NULL,
  event_type     VARCHAR(80)     NOT NULL,
  payload_sha256 CHAR(64)        NOT NULL,
  received_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_webhook_event (processor, event_id)
) ENGINE=InnoDB;

-- ─── Admin ────────────────────────────────────────────────────────────────
CREATE TABLE admin_users (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email         VARCHAR(254)    NOT NULL,
  password_hash CHAR(60)        NOT NULL,             -- bcrypt, cost >= 10
  display_name  VARCHAR(80)     NOT NULL,
  role          ENUM('admin','editor') NOT NULL DEFAULT 'editor',
  is_active     TINYINT(1)      NOT NULL DEFAULT 1,
  last_login_at DATETIME        NULL,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_admin_email (email)
) ENGINE=InnoDB;

CREATE TABLE admin_audit (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  admin_id    BIGINT UNSIGNED NULL,
  action      VARCHAR(80)     NOT NULL,               -- 'event.publish'
  entity_type VARCHAR(40)     NOT NULL,
  entity_id   BIGINT UNSIGNED NULL,
  before_json JSON            NULL,
  after_json  JSON            NULL,
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_entity (entity_type, entity_id),
  KEY idx_audit_admin_time (admin_id, created_at),
  CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id)
    REFERENCES admin_users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── Volunteers ───────────────────────────────────────────────────────────
CREATE TABLE volunteers (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  first_name   VARCHAR(80)     NOT NULL,
  last_name    VARCHAR(80)     NOT NULL,
  email        VARCHAR(254)    NOT NULL,
  phone        VARCHAR(32)     NULL,
  city         VARCHAR(100)    NULL,
  state        CHAR(2)         NULL,
  zip          VARCHAR(10)     NULL,
  availability VARCHAR(120)    NULL,
  skills       VARCHAR(500)    NULL,
  consent      TINYINT(1)      NOT NULL DEFAULT 0,
  status       ENUM('active','unsubscribed','blocked') NOT NULL DEFAULT 'active',
  source       ENUM('event','general','import')        NOT NULL DEFAULT 'general',
  created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_volunteer_email (email),
  KEY idx_volunteer_created (created_at),
  KEY idx_volunteer_status (status),
  CONSTRAINT chk_volunteer_consent CHECK (consent IN (0,1))
) ENGINE=InnoDB;

-- ─── Events ───────────────────────────────────────────────────────────────
CREATE TABLE events (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title         VARCHAR(160)    NOT NULL,
  slug          VARCHAR(180)    NOT NULL,
  summary       VARCHAR(280)    NULL,
  description   MEDIUMTEXT      NULL,
  category      ENUM('shoreline','river','oyster','education','other')
                                NOT NULL DEFAULT 'shoreline',
  location_name VARCHAR(160)    NULL,
  address       VARCHAR(200)    NULL,
  city          VARCHAR(100)    NULL,
  state         CHAR(2)         NULL,
  zip           VARCHAR(10)     NULL,
  map_url       VARCHAR(500)    NULL,
  starts_at     DATETIME        NOT NULL,             -- UTC
  ends_at       DATETIME        NULL,                 -- UTC
  timezone      VARCHAR(64)     NOT NULL DEFAULT 'America/New_York',
  capacity      SMALLINT UNSIGNED NOT NULL,
  seats_taken   SMALLINT UNSIGNED NOT NULL DEFAULT 0, -- guarded counter
  status        ENUM('draft','published','cancelled','completed')
                                NOT NULL DEFAULT 'draft',
  created_by    BIGINT UNSIGNED NULL,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_event_slug (slug),
  KEY idx_event_public (status, starts_at),
  KEY idx_event_created_by (created_by),
  CONSTRAINT fk_event_admin FOREIGN KEY (created_by)
    REFERENCES admin_users (id) ON DELETE SET NULL,
  CONSTRAINT chk_event_capacity CHECK (capacity > 0),
  CONSTRAINT chk_event_seats CHECK (seats_taken <= capacity),
  CONSTRAINT chk_event_window CHECK (ends_at IS NULL OR ends_at >= starts_at)
) ENGINE=InnoDB;

-- ─── Registrations ────────────────────────────────────────────────────────
CREATE TABLE event_registrations (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id         BIGINT UNSIGNED NOT NULL,
  volunteer_id     BIGINT UNSIGNED NOT NULL,
  status           ENUM('registered','waitlisted','cancelled','attended','no_show')
                                   NOT NULL DEFAULT 'registered',
  token_hash       CHAR(64)        NULL,              -- SHA-256(token), never the token
  token_expires_at DATETIME        NULL,
  notes            VARCHAR(500)    NULL,
  registered_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  waitlisted_at    DATETIME        NULL,              -- derived order: waitlisted_at, id
  cancelled_at     DATETIME        NULL,
  attended_at      DATETIME        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_registration (event_id, volunteer_id),
  KEY idx_reg_event_status (event_id, status, waitlisted_at, id),
  KEY idx_reg_volunteer (volunteer_id),
  KEY idx_reg_token (token_hash),
  CONSTRAINT fk_reg_event FOREIGN KEY (event_id)
    REFERENCES events (id) ON DELETE RESTRICT,
  CONSTRAINT fk_reg_volunteer FOREIGN KEY (volunteer_id)
    REFERENCES volunteers (id) ON DELETE RESTRICT,
  CONSTRAINT chk_reg_waitlisted CHECK (
    status <> 'waitlisted' OR waitlisted_at IS NOT NULL)
) ENGINE=InnoDB;

-- ─── Donations ────────────────────────────────────────────────────────────
CREATE TABLE donations (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reference           CHAR(36)        NOT NULL,       -- idempotency reference (UUID)
  donor_name          VARCHAR(120)    NULL,
  donor_email         VARCHAR(254)    NULL,
  amount_cents        INT UNSIGNED    NOT NULL,
  currency            CHAR(3)         NOT NULL DEFAULT 'USD',
  frequency           ENUM('one_time','monthly') NOT NULL DEFAULT 'one_time',
  processor           VARCHAR(32)     NOT NULL DEFAULT 'stripe',
  processor_session_id VARCHAR(128)   NULL,           -- Checkout Session
  processor_ref       VARCHAR(128)    NULL,           -- payment id from webhook
  status              ENUM('pending','succeeded','failed','refunded','abandoned')
                                      NOT NULL DEFAULT 'pending',
  is_anonymous        TINYINT(1)      NOT NULL DEFAULT 0,
  message             VARCHAR(280)    NULL,
  succeeded_at        DATETIME        NULL,
  created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_donation_reference (reference),
  UNIQUE KEY uq_donation_processor_ref (processor_ref),
  KEY idx_donation_status_time (status, created_at),
  CONSTRAINT chk_donation_amount CHECK (amount_cents BETWEEN 100 AND 1000000)
) ENGINE=InnoDB;

-- ─── Content ──────────────────────────────────────────────────────────────
CREATE TABLE partners (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(160)    NOT NULL,
  slug          VARCHAR(180)    NOT NULL,
  type          ENUM('nonprofit','corporate','government','community')
                                NOT NULL DEFAULT 'nonprofit',
  logo_path     VARCHAR(255)    NULL,
  website_url   VARCHAR(500)    NULL,
  blurb         VARCHAR(280)    NULL,
  display_order SMALLINT        NOT NULL DEFAULT 0,
  is_active     TINYINT(1)      NOT NULL DEFAULT 1,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_partner_slug (slug),
  KEY idx_partner_active_order (is_active, display_order)
) ENGINE=InnoDB;

CREATE TABLE initiatives (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title           VARCHAR(160)    NOT NULL,
  slug            VARCHAR(180)    NOT NULL,
  summary         VARCHAR(280)    NULL,
  body            MEDIUMTEXT      NULL,
  location        VARCHAR(160)    NULL,
  hero_image_path VARCHAR(255)    NULL,
  status          ENUM('draft','published') NOT NULL DEFAULT 'draft',
  display_order   SMALLINT        NOT NULL DEFAULT 0,
  created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_initiative_slug (slug),
  KEY idx_initiative_public (status, display_order)
) ENGINE=InnoDB;

-- ─── Newsletter ───────────────────────────────────────────────────────────
CREATE TABLE newsletter_subscribers (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email             VARCHAR(254)    NOT NULL,
  source            VARCHAR(60)     NULL,
  status            ENUM('pending','confirmed','unsubscribed')
                                    NOT NULL DEFAULT 'pending',
  token_hash        CHAR(64)        NULL,
  token_scope       ENUM('confirm','unsubscribe') NULL,
  token_expires_at  DATETIME        NULL,
  confirmed_at      DATETIME        NULL,
  unsubscribed_at   DATETIME        NULL,
  created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_subscriber_email (email),
  KEY idx_subscriber_token (token_hash),
  KEY idx_subscriber_status (status)
) ENGINE=InnoDB;

-- ─── Impact: definitions + facts ──────────────────────────────────────────
CREATE TABLE impact_metrics (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  metric_key    VARCHAR(60)     NOT NULL,             -- 'trash_lb'
  label         VARCHAR(120)    NOT NULL,
  unit          VARCHAR(32)     NOT NULL,             -- 'lb', 'mi', 'people'
  aggregation   ENUM('sum','latest') NOT NULL DEFAULT 'sum',
  category      ENUM('cleanup','conservation','community')
                                NOT NULL DEFAULT 'cleanup',
  is_featured   TINYINT(1)      NOT NULL DEFAULT 0,
  display_order SMALLINT        NOT NULL DEFAULT 0,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_metric_key (metric_key),
  KEY idx_metric_featured (is_featured, display_order)
) ENGINE=InnoDB;

CREATE TABLE impact_entries (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  metric_id   BIGINT UNSIGNED NOT NULL,
  event_id    BIGINT UNSIGNED NULL,
  occurred_on DATE            NOT NULL,
  value       DECIMAL(12,2)   NOT NULL,
  note        VARCHAR(280)    NULL,
  created_by  BIGINT UNSIGNED NULL,
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_entry_metric_date (metric_id, occurred_on),
  KEY idx_entry_event (event_id),
  KEY idx_entry_created_by (created_by),
  CONSTRAINT fk_entry_metric FOREIGN KEY (metric_id)
    REFERENCES impact_metrics (id) ON DELETE RESTRICT,
  CONSTRAINT fk_entry_event FOREIGN KEY (event_id)
    REFERENCES events (id) ON DELETE SET NULL,
  CONSTRAINT fk_entry_admin FOREIGN KEY (created_by)
    REFERENCES admin_users (id) ON DELETE SET NULL,
  CONSTRAINT chk_entry_value CHECK (value >= 0)
) ENGINE=InnoDB;

-- ─── Contact ──────────────────────────────────────────────────────────────
CREATE TABLE contact_messages (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(120)    NOT NULL,
  email       VARCHAR(254)    NOT NULL,
  phone       VARCHAR(32)     NULL,
  subject     ENUM('general','volunteer','partnership','press','donation')
                              NOT NULL DEFAULT 'general',
  message     TEXT            NOT NULL,
  status      ENUM('new','read','replied','spam') NOT NULL DEFAULT 'new',
  ip_hash     CHAR(64)        NULL,                   -- salted SHA-256, never the raw IP
  handled_by  BIGINT UNSIGNED NULL,
  handled_at  DATETIME        NULL,
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_message_status_time (status, created_at),
  CONSTRAINT fk_message_admin FOREIGN KEY (handled_by)
    REFERENCES admin_users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── Transactional outbox ─────────────────────────────────────────────────
CREATE TABLE email_outbox (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template        VARCHAR(60)     NOT NULL,
  recipient_email VARCHAR(254)    NOT NULL,
  subject         VARCHAR(200)    NOT NULL,
  body_text       MEDIUMTEXT      NOT NULL,
  related_type    VARCHAR(40)     NULL,               -- 'registration','donation'
  related_id      BIGINT UNSIGNED NULL,
  status          ENUM('queued','sending','sent','failed') NOT NULL DEFAULT 'queued',
  attempts        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  claim_token     CHAR(36)        NULL,               -- prevents double send
  claimed_at      DATETIME        NULL,
  next_attempt_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  message_id      VARCHAR(200)    NULL,
  last_error      VARCHAR(500)    NULL,
  sent_at         DATETIME        NULL,
  created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_outbox_claim (status, next_attempt_at, id),
  KEY idx_outbox_related (related_type, related_id)
) ENGINE=InnoDB;
