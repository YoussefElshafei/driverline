package com.driverline.planning.reference;

import com.driverline.planning.department.Department;

public record DepartmentResponse(String code, String name) {

    static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getCode(), department.getName());
    }
}
