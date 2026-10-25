package com.driverline.planning.budget;

import com.driverline.planning.account.Account;
import com.driverline.planning.department.Department;
import com.driverline.planning.period.FiscalPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** The planned amount for one account, in one department, for one month. */
@Entity
@Table(name = "budget_line")
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Embedded
    private FiscalPeriod period;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Required by JPA, which creates entities through reflection. */
    protected BudgetLine() {
    }

    public BudgetLine(Account account, Department department, FiscalPeriod period, BigDecimal amount) {
        this.account = account;
        this.department = department;
        this.period = period;
        this.amount = amount;
    }

    public void changeAmount(BigDecimal newAmount) {
        this.amount = newAmount;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Department getDepartment() {
        return department;
    }

    public FiscalPeriod getPeriod() {
        return period;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
