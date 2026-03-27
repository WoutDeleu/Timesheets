CREATE TABLE internal_activity (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_date   DATE          NOT NULL,
    hours           DECIMAL(4,2)  NOT NULL DEFAULT 7.6,
    description     VARCHAR(500)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
