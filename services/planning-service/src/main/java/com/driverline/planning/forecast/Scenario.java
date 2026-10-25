package com.driverline.planning.forecast;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Base, Best or Worst. Inserted by the V2 migration and never changed by the application. */
@Entity
@Table(name = "scenario")
public class Scenario {

    public static final String BASE = "BASE";
    public static final String BEST = "BEST";
    public static final String WORST = "WORST";

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Required by JPA, which creates entities through reflection. */
    protected Scenario() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
