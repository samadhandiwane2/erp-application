package com.erp.tenant.dto.section;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SectionResponse {
    private Long id;
    private Long classId;
    private String className;
    private String sectionName;
    private String sectionCode;
    private Integer maxStudents;
    private Integer currentStudents;
    private String roomNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

}
