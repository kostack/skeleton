ALTER TABLE todos
    ADD COLUMN project_id BIGINT NULL AFTER completed;

-- Existing todos are moved into a "Default" project, created only when there is something to move.
SET @default_project_identifier = UUID();

INSERT INTO projects (identifier, name, created_at, last_modified_at)
SELECT @default_project_identifier, 'Default', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM DUAL
WHERE EXISTS (SELECT 1 FROM todos);

UPDATE todos
SET project_id = (SELECT id FROM projects WHERE identifier = @default_project_identifier)
WHERE project_id IS NULL;

ALTER TABLE todos
    MODIFY COLUMN project_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_todos_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    ADD INDEX idx_todos_project_id (project_id);
