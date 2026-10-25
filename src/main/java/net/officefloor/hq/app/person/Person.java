package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private Statements statements = Statements.NONE;
    private LocalDate retiredFrom;
    private boolean held;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /** The person's statements: how many, and the last one's due-total and date. */
    public Statements getStatements() {
        return statements;
    }

    public void setStatements(Statements statements) {
        this.statements = statements;
    }

    /** The date the person is retired from; null when not retired. Set at all, the person counts as retired. */
    public LocalDate getRetiredFrom() {
        return retiredFrom;
    }

    public void setRetiredFrom(LocalDate retiredFrom) {
        this.retiredFrom = retiredFrom;
    }

    public boolean isRetired() {
        return retiredFrom != null;
    }

    /** Whether the person is on hold: their OPEN claims then count in no live total. */
    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }
}
