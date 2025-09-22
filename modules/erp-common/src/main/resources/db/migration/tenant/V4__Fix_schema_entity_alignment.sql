-- V4__Fix_schema_entity_alignment.sql
-- MySQL Compatible Version - Works with MySQL 5.7+

-- Create stored procedure to safely add columns
DELIMITER $$

DROP PROCEDURE IF EXISTS AddColumnIfNotExists$$
CREATE PROCEDURE AddColumnIfNotExists(
    IN tableName VARCHAR(100),
    IN columnName VARCHAR(100),
    IN columnDefinition VARCHAR(500)
)
BEGIN
    DECLARE column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = tableName
    AND COLUMN_NAME = columnName;

    IF column_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS AddIndexIfNotExists$$
CREATE PROCEDURE AddIndexIfNotExists(
    IN tableName VARCHAR(100),
    IN indexName VARCHAR(100),
    IN indexColumns VARCHAR(200)
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO index_exists
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = tableName
    AND INDEX_NAME = indexName;

    IF index_exists = 0 THEN
        SET @sql = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, '(', indexColumns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- Fix students table columns
-- =====================================================
CALL AddColumnIfNotExists('students', 'middle_name', 'VARCHAR(50) AFTER first_name');
CALL AddColumnIfNotExists('students', 'alternate_phone', 'VARCHAR(20) AFTER phone');
CALL AddColumnIfNotExists('students', 'admission_class_id', 'BIGINT AFTER admission_date');
CALL AddColumnIfNotExists('students', 'house', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'caste', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'aadhar_number', 'VARCHAR(12)');
CALL AddColumnIfNotExists('students', 'birth_certificate_number', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'previous_school', 'TEXT');
CALL AddColumnIfNotExists('students', 'previous_class', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_number', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_date', 'DATE');
CALL AddColumnIfNotExists('students', 'medical_conditions', 'TEXT');
CALL AddColumnIfNotExists('students', 'allergies', 'TEXT');
CALL AddColumnIfNotExists('students', 'emergency_contact_name', 'VARCHAR(100)');
CALL AddColumnIfNotExists('students', 'emergency_contact_phone', 'VARCHAR(20)');
CALL AddColumnIfNotExists('students', 'emergency_contact_relation', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transport_mode', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'route_id', 'BIGINT');
CALL AddColumnIfNotExists('students', 'bus_stop', 'VARCHAR(100)');
CALL AddColumnIfNotExists('students', 'profile_photo_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'birth_certificate_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'aadhar_card_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'remarks', 'TEXT');
CALL AddColumnIfNotExists('students', 'is_rte', 'BOOLEAN DEFAULT FALSE');

-- =====================================================
-- Fix guardians table columns
-- =====================================================
CALL AddColumnIfNotExists('guardians', 'annual_income', 'DECIMAL(12,2)');
CALL AddColumnIfNotExists('guardians', 'office_address', 'TEXT');
CALL AddColumnIfNotExists('guardians', 'office_phone', 'VARCHAR(20)');
CALL AddColumnIfNotExists('guardians', 'aadhar_number', 'VARCHAR(12)');
CALL AddColumnIfNotExists('guardians', 'can_pickup_child', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('guardians', 'photo_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('guardians', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('guardians', 'updated_by', 'BIGINT');

-- =====================================================
-- Fix student_class_history table columns
-- =====================================================
CALL AddColumnIfNotExists('student_class_history', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('student_class_history', 'updated_by', 'BIGINT');
CALL AddColumnIfNotExists('student_class_history', 'is_active', 'BOOLEAN DEFAULT TRUE');

-- =====================================================
-- Add audit columns to other tables
-- =====================================================
CALL AddColumnIfNotExists('academic_years', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('academic_years', 'updated_by', 'BIGINT');
CALL AddColumnIfNotExists('classes', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('classes', 'updated_by', 'BIGINT');
CALL AddColumnIfNotExists('sections', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('sections', 'updated_by', 'BIGINT');
CALL AddColumnIfNotExists('subjects', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('subjects', 'updated_by', 'BIGINT');

-- =====================================================
-- Create indexes for performance
-- =====================================================
CALL AddIndexIfNotExists('students', 'idx_student_aadhar', 'aadhar_number');
CALL AddIndexIfNotExists('students', 'idx_emergency_contact', 'emergency_contact_phone');
CALL AddIndexIfNotExists('students', 'idx_student_full_name', 'first_name, last_name');
CALL AddIndexIfNotExists('guardians', 'idx_guardian_phone', 'phone');

-- =====================================================
-- Clean up procedures
-- =====================================================
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
DROP PROCEDURE IF EXISTS AddIndexIfNotExists;