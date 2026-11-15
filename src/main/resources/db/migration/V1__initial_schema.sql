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

CREATE TABLE todos
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    identifier       VARCHAR(50)  NOT NULL,
    title            VARCHAR(100) NOT NULL,
    completed        BOOLEAN      NOT NULL DEFAULT FALSE,
    project_id       BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    created_by       VARCHAR(36)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_todos_identifier UNIQUE (identifier),
    CONSTRAINT uk_todos_title UNIQUE (title),
    CONSTRAINT fk_todos_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    INDEX idx_todos_created_by_last_modified_at (created_by, last_modified_at),
    INDEX idx_todos_completed (completed),
    INDEX idx_todos_project_id (project_id)
);
