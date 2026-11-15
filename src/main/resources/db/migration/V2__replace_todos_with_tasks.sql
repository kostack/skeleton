DROP TABLE todos;

ALTER TABLE projects
    ADD COLUMN description      VARCHAR(1000) NULL AFTER name,
    ADD COLUMN owner_id         VARCHAR(36)   NOT NULL AFTER description,
    ADD COLUMN featured_task_id BIGINT        NULL AFTER owner_id,
    ADD INDEX idx_projects_owner_id (owner_id);

CREATE TABLE tasks
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    identifier       VARCHAR(50)  NOT NULL,
    project_id       BIGINT       NOT NULL,
    title            VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    assignee_id      VARCHAR(36)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tasks_identifier UNIQUE (identifier),
    CONSTRAINT fk_tasks_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    INDEX idx_tasks_project_id_created_at (project_id, created_at),
    INDEX idx_tasks_project_id_status (project_id, status),
    INDEX idx_tasks_assignee_id (assignee_id)
);

-- A task can be featured by at most one project
ALTER TABLE projects
    ADD CONSTRAINT uk_projects_featured_task_id UNIQUE (featured_task_id),
    ADD CONSTRAINT fk_projects_featured_task_id FOREIGN KEY (featured_task_id) REFERENCES tasks (id) ON DELETE SET NULL;
