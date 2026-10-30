package net.officefloor.hq.app.person;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private int version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    private boolean held;

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
    /** The date the person is retired from; null when not retired. Set means retired, whatever the date. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether a claim may be dated on this date: never after the person's retirement date. */
    public boolean allowsClaimOn(LocalDate date) { return retiredFrom == null || !date.isAfter(retiredFrom); }
    /** Whether the person is on hold: their OPEN claims then count in no live total. */
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }
    /** Whether the claim counts in the live totals: OPEN or SETTLED, but not OPEN while the person is held. */
    public boolean countsLive(Claim claim) {
        return claim.getStatus().counts() && !(held && claim.getStatus() == ClaimStatus.OPEN);
    }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public void setName(String name) { this.name = name; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setVersion(int version) { this.version = version; }
}
