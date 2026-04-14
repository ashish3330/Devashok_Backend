-- Seed default organization
INSERT INTO organizations (name, code, address, phone, email, rera_number, is_active, created_at, updated_at)
SELECT 'DevAshok Enclave', 'DEVASHOK', 'Plot No. 42, Sector 18, Navi Mumbai', '+919876543210', 'admin@devashokenclave.in', 'MH/NAVI/2021/00342', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE code = 'DEVASHOK');

-- Seed construction-linked installment plan templates
INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'Registration of Title Deeds', 1, 15.00, 'Within 15 Days After Registration of Title Deeds: 15% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 1);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'DPC & Ground Floor Roof Slab', 2, 25.00, 'On Commencement of DPC and on commencement of Ground Floor Roof Slab: 25% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 2);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'First Floor Slab', 3, 15.00, 'On commencement of First Floor Slab: 15% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 3);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'First Mumty Slab', 4, 10.00, 'On commencement of First Mumty Slab: 10% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 4);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'GI/CI Piping', 5, 10.00, 'On commencement of G.I./C.I. piping: 10% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 5);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'Fixing Doors & Windows', 6, 10.00, 'On commencement of Fixing of Doors and Windows: 10% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 6);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'Internal & External Plastering', 7, 10.00, 'On commencement of internal and external plastering: 10% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 7);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'Flooring', 8, 2.50, 'On commencement of Flooring: 2.5% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 8);

INSERT INTO installment_plan_templates (phase_name, phase_order, percentage_of_total, description, is_active, created_at, updated_at)
SELECT 'Possession', 9, 2.50, 'At the Time of Possession: 2.5% of Construction Cost', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM installment_plan_templates WHERE phase_order = 9);
