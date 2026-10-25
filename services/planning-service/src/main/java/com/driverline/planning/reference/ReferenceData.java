package com.driverline.planning.reference;

import com.driverline.planning.account.Account;
import com.driverline.planning.department.Department;
import java.util.Map;
import java.util.Set;

/** All accounts and departments, indexed by code, so validation can check many rows without a query per row. */
public record ReferenceData(Map<String, Account> accountsByCode, Map<String, Department> departmentsByCode) {

    public ReferenceData {
        accountsByCode = Map.copyOf(accountsByCode);
        departmentsByCode = Map.copyOf(departmentsByCode);
    }

    public Set<String> accountCodes() {
        return accountsByCode.keySet();
    }

    public Set<String> departmentCodes() {
        return departmentsByCode.keySet();
    }

    public Account account(String code) {
        return accountsByCode.get(code);
    }

    public Department department(String code) {
        return departmentsByCode.get(code);
    }
}
