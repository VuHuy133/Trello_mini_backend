-- Query Performance Analysis Script
-- Run these EXPLAIN queries to verify indexes are being used

-- Check if dashboard stats query is using indexes
EXPLAIN SELECT COUNT(*) AS count FROM tasks WHERE status = 'TODO';
EXPLAIN SELECT COUNT(*) AS count FROM tasks WHERE dueDate < NOW() AND status != 'DONE';
EXPLAIN SELECT * FROM tasks WHERE dueDate < NOW() AND status != 'DONE' ORDER BY dueDate ASC LIMIT 10;

-- Check user queries
EXPLAIN SELECT COUNT(*) AS admin_count FROM users WHERE role = 'ADMIN';
EXPLAIN SELECT * FROM users ORDER BY createdAt DESC LIMIT 20;

-- Check project queries
EXPLAIN SELECT * FROM projects WHERE owner_id = 1 ORDER BY createdAt DESC LIMIT 20;

-- Complex queries with composite indexes
EXPLAIN SELECT * FROM tasks WHERE project_id = 1 AND status = 'DOING' LIMIT 20;

-- Query statistics (MySQL 8.0+)
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = 'tasks';
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = 'users';
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = 'projects';
