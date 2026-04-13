CREATE TABLE clock_session (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT        NOT NULL,
    session_date        DATE          NOT NULL,
    clock_in_time       TIMESTAMP     NOT NULL,
    clock_out_time      TIMESTAMP,
    break_start         TIMESTAMP,
    total_break_seconds BIGINT        NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    work_location       VARCHAR(20)   NOT NULL DEFAULT 'OFFICE',
    notes               VARCHAR(500),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clock_session_project FOREIGN KEY (project_id) REFERENCES project(id)
);
