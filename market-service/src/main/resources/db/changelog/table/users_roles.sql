CREATE SEQUENCE IF NOT EXISTS users_roles_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users_roles
(
    id      BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('users_roles_id_seq'),
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_users_id
    FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_roles_id
    FOREIGN KEY (role_id) REFERENCES roles (id)
);


INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.name = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;


INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.name = 'admin' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.name = 'user' AND r.name = 'USER'
ON CONFLICT DO NOTHING;