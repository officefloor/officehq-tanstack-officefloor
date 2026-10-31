package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person on a sheet. `id` is null until the person is first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    private String grade;
    private int version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, int version) {
        this.version = version;
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** How many times the person has been saved since added. */
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    /** The last statement's due-total; null when none was taken or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    /** The sheet's today when the last statement was taken; null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    /** The date the person is retired from; null when they are not retired. Set at all, they count
     *  as retired at once, whatever the date. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }

    /** Refuses a claim dated after the person's retirement date. */
    public void refuseRetired(LocalDate claimDate) throws PersonRetiredException {
        if (retiredFrom != null && claimDate.isAfter(retiredFrom)) {
            throw new PersonRetiredException(name, retiredFrom);
        }
    }
}
