-- V3__Add_student_history_tables.sql
-- MySQL Compatible Version - No IF NOT EXISTS in ALTER TABLE

-- Create procedure to safely add columns
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

DELIMITER ;

-- Add new columns to students table using the procedure
CALL AddColumnIfNotExists('students', 'middle_name', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'alternate_phone', 'VARCHAR(20)');
CALL AddColumnIfNotExists('students', 'admission_class_id', 'BIGINT');
CALL AddColumnIfNotExists('students', 'house', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'caste', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'birth_certificate_number', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'previous_class', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_number', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_date', 'DATE');
CALL AddColumnIfNotExists('students', 'allergies', 'TEXT');
CALL AddColumnIfNotExists('students', 'emergency_contact_relation', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'transport_mode', 'VARCHAR(50)');
CALL AddColumnIfNotExists('students', 'route_id', 'BIGINT');
CALL AddColumnIfNotExists('students', 'bus_stop', 'VARCHAR(100)');
CALL AddColumnIfNotExists('students', 'aadhar_card_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'remarks', 'TEXT');
CALL AddColumnIfNotExists('students', 'is_rte', 'BOOLEAN DEFAULT FALSE');

-- Additional columns needed by Student entity
CALL AddColumnIfNotExists('students', 'aadhar_number', 'VARCHAR(12)');
CALL AddColumnIfNotExists('students', 'previous_school', 'TEXT');
CALL AddColumnIfNotExists('students', 'medical_conditions', 'TEXT');
CALL AddColumnIfNotExists('students', 'emergency_contact_name', 'VARCHAR(100)');
CALL AddColumnIfNotExists('students', 'emergency_contact_phone', 'VARCHAR(20)');
CALL AddColumnIfNotExists('students', 'profile_photo_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'birth_certificate_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('students', 'transfer_certificate_url', 'VARCHAR(255)');

-- Add missing columns to guardians table
CALL AddColumnIfNotExists('guardians', 'office_address', 'TEXT');
CALL AddColumnIfNotExists('guardians', 'office_phone', 'VARCHAR(20)');
CALL AddColumnIfNotExists('guardians', 'aadhar_number', 'VARCHAR(12)');
CALL AddColumnIfNotExists('guardians', 'can_pickup_child', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('guardians', 'photo_url', 'VARCHAR(255)');
CALL AddColumnIfNotExists('guardians', 'created_by', 'BIGINT');
CALL AddColumnIfNotExists('guardians', 'updated_by', 'BIGINT');

-- Drop the procedure
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

-- Create student class history table
CREATE TABLE IF NOT EXISTS student_class_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    roll_number VARCHAR(20),
    start_date DATE NOT NULL,
    end_date DATE,
    promotion_status ENUM('PROMOTED', 'DETAINED', 'REPEAT', 'CONDITIONAL_PROMOTION', 'LEFT_SCHOOL', 'IN_PROGRESS') DEFAULT 'IN_PROGRESS',
    final_percentage DECIMAL(5,2),
    final_grade VARCHAR(5),
    attendance_percentage DECIMAL(5,2),
    total_working_days INT,
    days_present INT,
    rank_in_class INT,
    teacher_remarks TEXT,
    promoted_to_class_id BIGINT,
    promotion_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,

    INDEX idx_student_year (student_id, academic_year_id),
    INDEX idx_class_section (class_id, section_id),
    INDEX idx_promotion_status (promotion_status),

    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (section_id) REFERENCES sections(id),
    FOREIGN KEY (promoted_to_class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create student promotions table
CREATE TABLE IF NOT EXISTS student_promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    next_academic_year_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    from_class_id BIGINT NOT NULL,
    from_section_id BIGINT NOT NULL,
    to_class_id BIGINT,
    to_section_id BIGINT,
    promotion_type ENUM('REGULAR_PROMOTION', 'MID_YEAR_PROMOTION', 'DETENTION', 'DOUBLE_PROMOTION', 'SECTION_CHANGE', 'CONDITIONAL') NOT NULL,
    promotion_date DATE NOT NULL,
    reason TEXT,
    approved_by BIGINT,
    approved_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    INDEX idx_student_promotion (student_id, academic_year_id),
    INDEX idx_promotion_date (promotion_date),
    INDEX idx_from_class (from_class_id, from_section_id),
    INDEX idx_to_class (to_class_id, to_section_id),

    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    FOREIGN KEY (next_academic_year_id) REFERENCES academic_years(id),
    FOREIGN KEY (from_class_id) REFERENCES classes(id),
    FOREIGN KEY (from_section_id) REFERENCES sections(id),
    FOREIGN KEY (to_class_id) REFERENCES classes(id),
    FOREIGN KEY (to_section_id) REFERENCES sections(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create student academic info table
CREATE TABLE IF NOT EXISTS student_academic_info (
    student_id BIGINT PRIMARY KEY,
    admission_test_score INT,
    admission_test_rank INT,
    learning_disability TEXT,
    special_needs TEXT,
    counseling_required BOOLEAN DEFAULT FALSE,
    remedial_required BOOLEAN DEFAULT FALSE,
    gifted_student BOOLEAN DEFAULT FALSE,
    sports_quota BOOLEAN DEFAULT FALSE,
    scholarship_holder BOOLEAN DEFAULT FALSE,
    scholarship_details TEXT,
    extracurricular_activities TEXT,
    achievements TEXT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,

    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create view for current class students
CREATE OR REPLACE VIEW current_class_students AS
SELECT
    s.*,
    c.class_name,
    sec.section_name,
    ay.year_name as academic_year
FROM students s
LEFT JOIN classes c ON s.current_class_id = c.id
LEFT JOIN sections sec ON s.current_section_id = sec.id
LEFT JOIN academic_years ay ON s.academic_year_id = ay.id
WHERE s.is_active = TRUE;