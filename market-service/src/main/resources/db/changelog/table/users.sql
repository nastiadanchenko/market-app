CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS users
(
    id          BIGINT PRIMARY KEY    DEFAULT nextval('users_id_seq'),
    name        VARCHAR(100) NOT NULL UNIQUE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    keycloak_id uuid         not null unique,
    email       VARCHAR(255)
);
