-- OWNER: Sufiyan. Demo data. Run as BLMS after 02_schema.sql, then COMMIT.
-- Logins (passwords are BCrypt hashes - UC01):  admin / admin123  (ADMIN)    rep / rep123  (SALES_REP)
INSERT INTO workspaces (company_name, target_industry, services_offered, smtp_port)
VALUES ('Demo Company', 'Tech', 'Custom software development', 587);

INSERT INTO users (workspace_id, username, password_hash, role, is_active)
VALUES ((SELECT MIN(workspace_id) FROM workspaces), 'admin', '$2a$10$GOghxFrqSuiVyx/LqpmkAOAcrpvZpyLryQM3/0W2bdQZGpwuR.u7e', 'ADMIN', 1);

INSERT INTO users (workspace_id, username, password_hash, role, is_active)
VALUES ((SELECT MIN(workspace_id) FROM workspaces), 'rep', '$2a$10$orpMhWUEQufzPkaTVmINP.bTCQ7nRAITXqkNSVPo.C0bZ73yX0iMK', 'SALES_REP', 1);

COMMIT;
