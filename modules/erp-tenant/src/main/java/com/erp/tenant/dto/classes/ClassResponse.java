package com.erp.tenant.dto.classes;

import com.erp.tenant.dto.section.SectionResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassResponse {
    private Long id;
    private String className;
    private String classCode;
    private Integer gradeLevel;
    private String description;
    private Integer maxStudents;
    private Integer currentStudents;
    private List<SectionResponse> sections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
