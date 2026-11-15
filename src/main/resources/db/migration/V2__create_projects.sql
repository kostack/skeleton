CREATE TABLE projects
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    identifier       VARCHAR(50)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_projects_identifier UNIQUE (identifier),
    INDEX idx_projects_name (name),
    INDEX idx_projects_created_at (created_at)
);
