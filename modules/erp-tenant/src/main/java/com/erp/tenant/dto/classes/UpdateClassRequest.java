package com.erp.tenant.dto.classes;

import lombok.Data;

@Data
public class UpdateClassRequest {
    private String className;
    private String description;
    private Integer maxStudents;
    private Boolean isActive;

}
