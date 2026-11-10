-- Initialize the monotonic public-content cache version on existing and new databases.
-- Preserve an existing version: it may already reflect legitimate content writes.
INSERT INTO app_meta (meta_key, meta_value) VALUES ('cache_version', '1')
ON DUPLICATE KEY UPDATE meta_value = meta_value;
