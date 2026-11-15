CREATE TABLE todos
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    identifier       VARCHAR(50)  NOT NULL,
    title            VARCHAR(100) NOT NULL,
    completed        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    created_by       VARCHAR(36)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_todos_identifier UNIQUE (identifier),
    CONSTRAINT uk_todos_title UNIQUE (title),
    INDEX idx_todos_created_by_last_modified_at (created_by, last_modified_at),
    INDEX idx_todos_completed (completed)
);
