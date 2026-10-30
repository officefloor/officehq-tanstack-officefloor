package net.officefloor.hq.app.person;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
import java.math.BigDecimal;
import java.time.LocalDate;

public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private int version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;

    public Person(long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(long id, String sheetId, String name, String grade, int version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public int getVersion() { return version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    /** The last statement's due-total; null when none was taken or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    /** The sheet's today when the last statement was taken; null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public void setName(String name) { this.name = name; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setVersion(int version) { this.version = version; }
}
