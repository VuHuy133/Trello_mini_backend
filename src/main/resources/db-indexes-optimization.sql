-- Database Optimization Script for Trello Mini
-- Create indexes to improve query performance for large datasets

-- Users table indexes
ALTER TABLE users ADD INDEX idx_role (role);
ALTER TABLE users ADD INDEX idx_username_unique (username);
ALTER TABLE users ADD INDEX idx_email_unique (email);
ALTER TABLE users ADD INDEX idx_created_at (createdAt);

-- Projects table indexes
ALTER TABLE projects ADD INDEX idx_owner_id (owner_id);
ALTER TABLE projects ADD INDEX idx_project_owner_created (owner_id, createdAt);
ALTER TABLE projects ADD INDEX idx_type (type);
ALTER TABLE projects ADD INDEX idx_created_at (createdAt);

-- Tasks table indexes (critical for performance)
ALTER TABLE tasks ADD INDEX idx_project_id (project_id);
ALTER TABLE tasks ADD INDEX idx_user_id (user_id);
ALTER TABLE tasks ADD INDEX idx_assignee_id (assignee_id);
ALTER TABLE tasks ADD INDEX idx_status (status);
ALTER TABLE tasks ADD INDEX idx_priority (priority);
ALTER TABLE tasks ADD INDEX idx_project_status (project_id, status);
ALTER TABLE tasks ADD INDEX idx_duedate (dueDate);
ALTER TABLE tasks ADD INDEX idx_status_duedate (status, dueDate);

-- ProjectMember table indexes
ALTER TABLE project_member ADD INDEX idx_project_id (project_id);
ALTER TABLE project_member ADD INDEX idx_user_id (user_id);

-- Verify indexes were created
SHOW INDEX FROM users;
SHOW INDEX FROM projects;
SHOW INDEX FROM tasks;
SHOW INDEX FROM project_member;
