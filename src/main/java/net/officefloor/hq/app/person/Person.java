package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person on a sheet. Mutable so an Apply step changes it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this(id, sheetId, name, grade, version, 0, null, null);
    }

    public Person(long id, String sheetId, String name, String grade, long version, int statementCount,
            BigDecimal statedTotal, LocalDate statementDate) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
        this.statementCount = statementCount;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** The version as loaded from the store; moves on once the person is saved. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    /** The last statement's due-total: null when none was taken, or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    /** The sheet's today when the last statement was taken: null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    /** The date the person is retired from: null when they are not retired. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    /** Whether a claim of the person may be dated this day: not after their retirement date. */
    public boolean claimAllowedOn(LocalDate date) { return retiredFrom == null || !date.isAfter(retiredFrom); }
}
