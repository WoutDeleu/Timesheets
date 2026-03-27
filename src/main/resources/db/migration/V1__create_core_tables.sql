-- V1: Core schema for Timesheets

CREATE TABLE project (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    daily_hour_target DECIMAL(4,2),
    billable    BOOLEAN       NOT NULL DEFAULT TRUE,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entry (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_date  DATE          NOT NULL,
    project_id  BIGINT        NOT NULL,
    hours_worked DECIMAL(4,2) NOT NULL,
    start_time  TIME,
    end_time    TIME,
    notes       VARCHAR(500),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_entry_project FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT uq_time_entry_date_project UNIQUE (entry_date, project_id)
);

CREATE TABLE leave_entry (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_date  DATE          NOT NULL,
    leave_type  VARCHAR(50)   NOT NULL,
    hours       DECIMAL(4,2)  NOT NULL DEFAULT 7.6,
    notes       VARCHAR(500),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_entry_date UNIQUE (entry_date)
);

CREATE TABLE holiday (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE         NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    holiday_type VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL',
    editable    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_holiday_date UNIQUE (holiday_date)
);

CREATE TABLE app_setting (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100)  NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    CONSTRAINT uq_setting_key UNIQUE (setting_key)
);

CREATE TABLE saldo_snapshot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE        NOT NULL,
    saldo_type  VARCHAR(20)   NOT NULL,
    project_id  BIGINT,
    balance     DECIMAL(8,2)  NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saldo_project FOREIGN KEY (project_id) REFERENCES project(id)
);

-- Default settings
INSERT INTO app_setting (setting_key, setting_value) VALUES ('default_daily_hours', '7.6');
INSERT INTO app_setting (setting_key, setting_value) VALUES ('vacation_days_per_year', '20');
INSERT INTO app_setting (setting_key, setting_value) VALUES ('adv_daily_surplus_hours', '0.4');
INSERT INTO app_setting (setting_key, setting_value) VALUES ('adv_day_hours', '7.6');
