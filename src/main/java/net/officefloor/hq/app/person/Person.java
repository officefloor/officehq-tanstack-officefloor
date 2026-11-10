package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A person on a sheet, as the store holds it. {@code id} is null until first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private int statementCount;
    private String statedTotal;
    private LocalDate statementDate;

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** Raised by one on each save; an edit names the version it was made from. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    /** How many statements have been taken for them. */
    public int getStatementCount() { return statementCount; }
    /** The last statement's due-total (n/a when it had none); null before the first. */
    public String getStatedTotal() { return statedTotal; }
    /** The sheet's today when the last statement was taken; null before the first. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatements(int count, String statedTotal, LocalDate statementDate) {
        this.statementCount = count;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }
}
