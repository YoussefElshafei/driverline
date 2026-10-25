package com.driverline.planning.forecast;

import com.driverline.planning.account.Account;
import com.driverline.planning.department.Department;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * How one account in one department is forecast for a fiscal year, with its driver values per scenario.
 * The values are owned by the line: saving the line saves them, and removing one from the list deletes it.
 */
@Entity
@Table(name = "forecast_line")
public class ForecastLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ForecastMethod method;

    @Column(name = "quantity_label", length = 60)
    private String quantityLabel;

    @Column(name = "rate_label", length = 60)
    private String rateLabel;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForecastDriverValue> values = new ArrayList<>();

    /** Required by JPA, which creates entities through reflection. */
    protected ForecastLine() {
    }

    public ForecastLine(int fiscalYear, Account account, Department department, ForecastMethod method,
            String quantityLabel, String rateLabel) {
        this.fiscalYear = fiscalYear;
        this.account = account;
        this.department = department;
        this.method = method;
        this.quantityLabel = quantityLabel;
        this.rateLabel = rateLabel;
    }

    public void changeMethod(ForecastMethod newMethod, String newQuantityLabel, String newRateLabel) {
        this.method = newMethod;
        this.quantityLabel = newQuantityLabel;
        this.rateLabel = newRateLabel;
        if (newMethod == ForecastMethod.BUDGET) {
            values.clear();
        }
    }

    /** Creates or replaces this line's driver values for one scenario. */
    public void setValues(Scenario scenario, BigDecimal quantity, BigDecimal rate, BigDecimal percent) {
        valueFor(scenario.getCode()).ifPresentOrElse(
                existing -> existing.update(quantity, rate, percent),
                () -> values.add(new ForecastDriverValue(this, scenario, quantity, rate, percent)));
    }

    public Optional<ForecastDriverValue> valueFor(String scenarioCode) {
        return values.stream().filter(value -> value.getScenario().getCode().equals(scenarioCode)).findFirst();
    }

    public Long getId() {
        return id;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public Account getAccount() {
        return account;
    }

    public Department getDepartment() {
        return department;
    }

    public ForecastMethod getMethod() {
        return method;
    }

    public String getQuantityLabel() {
        return quantityLabel;
    }

    public String getRateLabel() {
        return rateLabel;
    }

    public List<ForecastDriverValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}
