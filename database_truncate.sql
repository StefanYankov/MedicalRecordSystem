-- Select the database to use. This is the crucial first step.
USE medical_db;

-- Disable foreign key checks to allow truncation in any order
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate all application-specific tables
TRUNCATE TABLE visits;
TRUNCATE TABLE sick_leaves;
TRUNCATE TABLE treatments;
TRUNCATE TABLE medicines;
TRUNCATE TABLE patients;
TRUNCATE TABLE doctor_specialties;
TRUNCATE TABLE doctors;
TRUNCATE TABLE specialties;
TRUNCATE TABLE diagnoses;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
