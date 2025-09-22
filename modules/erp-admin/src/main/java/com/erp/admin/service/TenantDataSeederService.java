package com.erp.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDataSeederService {

    public void seedInitialData(DataSource tenantDataSource, String schemaName) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(tenantDataSource);

            log.info("Seeding initial data for schema: {}", schemaName);
            // Seed Academic Year
            seedAcademicYear(jdbcTemplate);
            // Seed Classes and Sections
            seedClassesAndSections(jdbcTemplate);
            // Seed Subjects
            seedSubjects(jdbcTemplate);
            // Seed Fee Categories
            seedFeeCategories(jdbcTemplate);
            // Seed Exam Types
            seedExamTypes(jdbcTemplate);

            log.info("Successfully seeded initial data for schema: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to seed initial data for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to seed initial data", e);
        }
    }

    private void seedAcademicYear(JdbcTemplate jdbcTemplate) {
        // Create current academic year
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        String yearName = currentYear + "-" + (currentYear + 1);
        LocalDate startDate = LocalDate.of(currentYear, 4, 1); // April 1st
        LocalDate endDate = LocalDate.of(currentYear + 1, 3, 31); // March 31st next year

        jdbcTemplate.update(
                "INSERT INTO academic_years (year_name, start_date, end_date, is_current, description, created_at, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?)",
                yearName, startDate, endDate, true, "Current Academic Year", true
        );

        log.info("Seeded academic year: {}", yearName);
    }

    private void seedClassesAndSections(JdbcTemplate jdbcTemplate) {
        // Define standard classes
        String[][] classes = {
                {"Nursery", "NUR", "0"},
                {"LKG", "LKG", "0"},
                {"UKG", "UKG", "0"},
                {"Class I", "CLS1", "1"},
                {"Class II", "CLS2", "2"},
                {"Class III", "CLS3", "3"},
                {"Class IV", "CLS4", "4"},
                {"Class V", "CLS5", "5"},
                {"Class VI", "CLS6", "6"},
                {"Class VII", "CLS7", "7"},
                {"Class VIII", "CLS8", "8"},
                {"Class IX", "CLS9", "9"},
                {"Class X", "CLS10", "10"},
                {"Class XI", "CLS11", "11"},
                {"Class XII", "CLS12", "12"}
        };

        for (String[] classData : classes) {
            // Insert class
            jdbcTemplate.update(
                    "INSERT INTO classes (class_name, class_code, grade_level, max_students, created_at, is_active) " +
                            "VALUES (?, ?, ?, ?, NOW(), ?)",
                    classData[0], classData[1], Integer.parseInt(classData[2]), 50, true
            );

            // Get the inserted class ID
            Long classId = jdbcTemplate.queryForObject(
                    "SELECT LAST_INSERT_ID()", Long.class
            );

            // Insert sections A, B for each class
            String[] sections = {"A", "B"};
            for (String section : sections) {
                jdbcTemplate.update(
                        "INSERT INTO sections (class_id, section_name, section_code, max_students, created_at, is_active) " +
                                "VALUES (?, ?, ?, ?, NOW(), ?)",
                        classId, section, section, 30, true
                );
            }
        }

        log.info("Seeded {} classes with sections", classes.length);
    }

    private void seedSubjects(JdbcTemplate jdbcTemplate) {
        String[][] subjects = {
                {"English", "ENG", "CORE", "2"},
                {"Hindi", "HIN", "CORE", "2"},
                {"Mathematics", "MATH", "CORE", "3"},
                {"Science", "SCI", "CORE", "3"},
                {"Social Studies", "SST", "CORE", "2"},
                {"Computer Science", "CS", "ELECTIVE", "2"},
                {"Physical Education", "PE", "ELECTIVE", "1"},
                {"Art & Craft", "ART", "EXTRA_CURRICULAR", "1"},
                {"Music", "MUS", "EXTRA_CURRICULAR", "1"},
                {"Environmental Studies", "EVS", "CORE", "2"}
        };

        for (String[] subjectData : subjects) {
            jdbcTemplate.update(
                    "INSERT INTO subjects (subject_name, subject_code, subject_type, credit_hours, created_at, is_active) " +
                            "VALUES (?, ?, ?, ?, NOW(), ?)",
                    subjectData[0], subjectData[1], subjectData[2],
                    Integer.parseInt(subjectData[3]), true
            );
        }

        log.info("Seeded {} subjects", subjects.length);
    }

    private void seedFeeCategories(JdbcTemplate jdbcTemplate) {
        String[][] feeCategories = {
                {"Tuition Fee", "TUITION", "TUITION", "1"},
                {"Admission Fee", "ADMISSION", "MISCELLANEOUS", "1"},
                {"Library Fee", "LIBRARY", "LIBRARY", "1"},
                {"Laboratory Fee", "LAB", "LABORATORY", "1"},
                {"Sports Fee", "SPORTS", "MISCELLANEOUS", "0"},
                {"Transport Fee", "TRANSPORT", "TRANSPORT", "0"},
                {"Examination Fee", "EXAM", "EXAM", "1"},
                {"Annual Charges", "ANNUAL", "MISCELLANEOUS", "1"},
                {"Development Fee", "DEVELOPMENT", "MISCELLANEOUS", "0"}
        };

        for (String[] feeData : feeCategories) {
            jdbcTemplate.update(
                    "INSERT INTO fee_categories (category_name, category_code, fee_type, is_mandatory, created_at, is_active) " +
                            "VALUES (?, ?, ?, ?, NOW(), ?)",
                    feeData[0], feeData[1], feeData[2],
                    feeData[3].equals("1"), true
            );
        }

        log.info("Seeded {} fee categories", feeCategories.length);
    }

    private void seedExamTypes(JdbcTemplate jdbcTemplate) {
        String[][] examTypes = {
                {"Unit Test 1", "UT1", "15.0"},
                {"Unit Test 2", "UT2", "15.0"},
                {"Mid Term Exam", "MID", "30.0"},
                {"Final Exam", "FINAL", "40.0"}
        };

        for (String[] examData : examTypes) {
            jdbcTemplate.update(
                    "INSERT INTO exam_types (type_name, type_code, weightage, created_at, is_active) " +
                            "VALUES (?, ?, ?, NOW(), ?)",
                    examData[0], examData[1], Double.parseDouble(examData[2]), true
            );
        }

        log.info("Seeded {} exam types", examTypes.length);
    }

}
