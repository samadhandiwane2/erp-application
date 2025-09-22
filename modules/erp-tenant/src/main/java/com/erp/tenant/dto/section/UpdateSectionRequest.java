package com.erp.tenant.dto.section;

import lombok.Data;

@Data
public class UpdateSectionRequest {
    private String sectionName;
    private Integer maxStudents;
    private String roomNumber;
    private Boolean isActive;

}
