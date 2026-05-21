CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

DELETE FROM todo;
ALTER TABLE todo ADD COLUMN user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_todo_user_id ON todo(user_id);
