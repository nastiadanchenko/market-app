CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT PRIMARY KEY    DEFAULT nextval('users_id_seq'),
    name       VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    keycloak_id text
);

INSERT INTO users (name, password)
VALUES ('admin', '$2a$12$o64/DaERZkR2TiRIiRSwR.P7e3m.t7bC7etrrkSBdVqKKRFnOnIeO'), -- пароль: 'password' в BCrypt
       ('user', '$2a$12$26FybbWceBE54oypcKhwKu2KmEcFU.cShxH4knijfsRepdYwc/kUK') -- пароль: 'user123' в BCrypt
ON CONFLICT (name) DO NOTHING;