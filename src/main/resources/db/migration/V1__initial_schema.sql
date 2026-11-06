CREATE DATABASE IF NOT EXISTS `skeleton`;
CREATE TABLE users
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    identity VARCHAR(36)  NOT NULL UNIQUE,
    roles    TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
