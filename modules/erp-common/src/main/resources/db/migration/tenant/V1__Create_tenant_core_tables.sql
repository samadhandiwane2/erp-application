-- Core School ERP tables for each tenant

-- Academic Structure
CREATE TABLE academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_year_name (year_name),
    INDEX idx_is_current (is_current),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(50) NOT NULL,
    class_code VARCHAR(20) NOT NULL,
    grade_level INT NOT NULL,
    description TEXT,
    max_students INT DEFAULT 50,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_class_code (class_code),
    INDEX idx_class_name (class_name),
    INDEX idx_grade_level (grade_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    section_name VARCHAR(20) NOT NULL,
    section_code VARCHAR(20) NOT NULL,
    max_students INT DEFAULT 30,
    room_number VARCHAR(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (class_id) REFERENCES classes(id),
    UNIQUE KEY uk_class_section (class_id, section_code),
    INDEX idx_section_name (section_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_name VARCHAR(100) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    description TEXT,
    credit_hours INT DEFAULT 1,
    subject_type ENUM('CORE', 'ELECTIVE', 'EXTRA_CURRICULAR') DEFAULT 'CORE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_subject_code (subject_code),
    INDEX idx_subject_name (subject_name),
    INDEX idx_subject_type (subject_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Staff Management
CREATE TABLE staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT, -- References master.users table
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    date_of_birth DATE,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    postal_code VARCHAR(10),
    country VARCHAR(50) DEFAULT 'India',
    hire_date DATE NOT NULL,
    job_title VARCHAR(100),
    department VARCHAR(100),
    salary DECIMAL(10,2),
    status ENUM('ACTIVE', 'INACTIVE', 'TERMINATED') DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_employee_id (employee_id),
    INDEX idx_email (email),
    INDEX idx_department (department),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Student Management
CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_number VARCHAR(20) NOT NULL UNIQUE,
    roll_number VARCHAR(20),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    postal_code VARCHAR(10),
    country VARCHAR(50) DEFAULT 'India',
    admission_date DATE NOT NULL,
    current_class_id BIGINT,
    current_section_id BIGINT,
    academic_year_id BIGINT,
    blood_group VARCHAR(5),
    religion VARCHAR(50),
    category VARCHAR(20),
    nationality VARCHAR(50) DEFAULT 'Indian',
    mother_tongue VARCHAR(50),
    student_status ENUM('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'GRADUATED', 'DROPPED') DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (current_class_id) REFERENCES classes(id),
    FOREIGN KEY (current_section_id) REFERENCES sections(id),
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    INDEX idx_admission_number (admission_number),
    INDEX idx_roll_number (roll_number),
    INDEX idx_name (first_name, last_name),
    INDEX idx_class_section (current_class_id, current_section_id),
    INDEX idx_status (student_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Guardian/Parent Information
CREATE TABLE guardians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    guardian_type ENUM('FATHER', 'MOTHER', 'GUARDIAN', 'OTHER') NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    relationship VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    occupation VARCHAR(100),
    annual_income DECIMAL(12,2),
    address TEXT,
    is_primary_contact BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_student_id (student_id),
    INDEX idx_guardian_type (guardian_type),
    INDEX idx_primary_contact (is_primary_contact)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Fee Structure
CREATE TABLE fee_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    fee_type ENUM('TUITION', 'LIBRARY', 'LABORATORY', 'TRANSPORT', 'EXAM', 'MISCELLANEOUS') NOT NULL,
    is_mandatory BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_category_name (category_name),
    INDEX idx_fee_type (fee_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_structures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    fee_category_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    due_date_offset_days INT DEFAULT 30, -- Days from admission/term start
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (fee_category_id) REFERENCES fee_categories(id),
    UNIQUE KEY uk_year_class_category (academic_year_id, class_id, fee_category_id),
    INDEX idx_academic_year (academic_year_id),
    INDEX idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;