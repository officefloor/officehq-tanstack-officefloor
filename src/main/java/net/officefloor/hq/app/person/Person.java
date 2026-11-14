package net.officefloor.hq.app.person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/** A person on a sheet: a name and a grade (A, B or C), the version raised by each saved edit, and
 *  their statements: how many have been taken, and the last one's due-total and date (none until the
 *  first), and the date they are retired from (null: not retired; set at all, they count as retired),
 *  and whether they are on hold (their OPEN claims then count in no live total). */
public class Person {
    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private int statementCount;
    private String statedTotal;
    private String statementDate;
    private String retiredFrom;
    private boolean held;
    /** How many claims the statement this request took bound, for its audit line. */
    private int statedClaims;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this(id, sheetId, name, grade, version, 0, null, null);
    }

    public Person(long id, String sheetId, String name, String grade, long version,
            int statementCount, String statedTotal, String statementDate) {
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
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public int getStatementCount() { return statementCount; }
    public String getStatedTotal() { return statedTotal; }
    public String getStatementDate() { return statementDate; }
    public String getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(String retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }

    /** Whether the claim counts in live totals: it counts by status, and is not an OPEN claim of a
     *  person on hold. */
    public boolean countsLive(Claim claim) {
        return claim.isCounting()
                && !(held && ClaimStatus.OPEN.name().equals(claim.getStatus()));
    }

    /** Whether a claim on this date is refused: the person is retired from a date before it. */
    public boolean retiredBefore(java.time.LocalDate date) {
        return retiredFrom != null && date.isAfter(java.time.LocalDate.parse(retiredFrom));
    }

    /** Record a statement just taken: its due-total (null: n/a), date and how many claims it bound. */
    public void stated(String total, String date, int claims) {
        this.statementCount++;
        this.statedTotal = total == null ? "n/a" : total;
        this.statementDate = date;
        this.statedClaims = claims;
    }

    @JsonIgnore
    public int getStatedClaims() { return statedClaims; }
}
