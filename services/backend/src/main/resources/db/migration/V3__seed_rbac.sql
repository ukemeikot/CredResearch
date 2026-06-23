-- Phase 1 seed: roles, permissions, role→permission mappings, demo institution.
-- Idempotent via ON CONFLICT so re-running on a clean DB is safe.

INSERT INTO roles (code, description) VALUES
    ('STUDENT',          'Research student / project owner'),
    ('SUPERVISOR',       'Reviews and supervises student projects'),
    ('CONSULTANT',       'Independent research consultant'),
    ('DEPARTMENT_ADMIN', 'Administers a department'),
    ('INSTITUTION_ADMIN','Administers an institution'),
    ('PLATFORM_ADMIN',   'CredResearch platform operator')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, description) VALUES
    ('USER_READ',          'View users within the tenant'),
    ('USER_MANAGE',        'Create/update users'),
    ('ROLE_ASSIGN',        'Assign roles to users'),
    ('USER_SUSPEND',       'Suspend/reactivate users'),
    ('INSTITUTION_READ',   'View institution'),
    ('INSTITUTION_MANAGE', 'Create/update institutions'),
    ('DEPARTMENT_READ',    'View departments'),
    ('DEPARTMENT_MANAGE',  'Create/update departments'),
    ('STUDENT_IMPORT',     'Bulk-import students')
ON CONFLICT (code) DO NOTHING;

-- PLATFORM_ADMIN → every permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;

-- INSTITUTION_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'USER_READ','USER_MANAGE','ROLE_ASSIGN','USER_SUSPEND',
    'INSTITUTION_READ','INSTITUTION_MANAGE','DEPARTMENT_READ','DEPARTMENT_MANAGE','STUDENT_IMPORT')
WHERE r.code = 'INSTITUTION_ADMIN'
ON CONFLICT DO NOTHING;

-- DEPARTMENT_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'USER_READ','DEPARTMENT_READ','DEPARTMENT_MANAGE')
WHERE r.code = 'DEPARTMENT_ADMIN'
ON CONFLICT DO NOTHING;

-- SUPERVISOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('USER_READ')
WHERE r.code = 'SUPERVISOR'
ON CONFLICT DO NOTHING;

-- Demo institution for onboarding/sales (FR-ORG-6)
INSERT INTO institutions (name, country, type, is_personal_tenant, status)
SELECT 'CredResearch Demo University', 'NG', 'university', false, 'active'
WHERE NOT EXISTS (SELECT 1 FROM institutions WHERE name = 'CredResearch Demo University');
