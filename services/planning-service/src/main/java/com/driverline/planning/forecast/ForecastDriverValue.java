package com.driverline.planning.forecast;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** The driver values of one forecast line in one scenario. Unused columns for the line's method are null. */
@Entity
@Table(name = "forecast_driver_value")
public class ForecastDriverValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forecast_line_id", nullable = false)
    private ForecastLine line;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(precision = 7, scale = 4)
    private BigDecimal percent;

    /** Required by JPA, which creates entities through reflection. */
    protected ForecastDriverValue() {
    }

    ForecastDriverValue(ForecastLine line, Scenario scenario, BigDecimal quantity, BigDecimal rate, BigDecimal percent) {
        this.line = line;
        this.scenario = scenario;
        this.quantity = quantity;
        this.rate = rate;
        this.percent = percent;
    }

    void update(BigDecimal newQuantity, BigDecimal newRate, BigDecimal newPercent) {
        this.quantity = newQuantity;
        this.rate = newRate;
        this.percent = newPercent;
    }

    public Long getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getPercent() {
        return percent;
    }
}
