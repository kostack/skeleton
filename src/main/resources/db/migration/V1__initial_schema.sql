CREATE DATABASE IF NOT EXISTS `skeleton`;
CREATE TABLE users
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    identity VARCHAR(36)  NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    roles    TEXT,
    PRIMARY KEY (id)
);
