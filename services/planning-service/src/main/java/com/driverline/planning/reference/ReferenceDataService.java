package com.driverline.planning.reference;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountRepository;
import com.driverline.planning.department.Department;
import com.driverline.planning.department.DepartmentRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private static final Sort BY_CODE = Sort.by("code");

    private final AccountRepository accountRepository;
    private final DepartmentRepository departmentRepository;

    public ReferenceDataService(AccountRepository accountRepository, DepartmentRepository departmentRepository) {
        this.accountRepository = accountRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll(BY_CODE).stream().map(AccountResponse::from).toList();
    }

    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll(BY_CODE).stream().map(DepartmentResponse::from).toList();
    }

    public ReferenceData load() {
        return new ReferenceData(
                accountRepository.findAll().stream()
                        .collect(Collectors.toMap(Account::getCode, Function.identity())),
                departmentRepository.findAll().stream()
                        .collect(Collectors.toMap(Department::getCode, Function.identity())));
    }
}
