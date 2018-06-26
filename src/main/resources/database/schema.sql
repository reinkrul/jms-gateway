create table jms_message(
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
  data TEXT not null,
  queue VARCHAR(100) NOT NULL,
  max_retries INTEGER NOT NULL DEFAULT 5,
  retries INTEGER NOT NULL DEFAULT 0,
  last_change TIMESTAMP
);