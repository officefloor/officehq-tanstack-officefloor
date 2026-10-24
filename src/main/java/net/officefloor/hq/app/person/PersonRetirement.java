package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One of a person's past retirements: from a date to the day they were unretired. */
@Embeddable
public class PersonRetirement {

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    /** The sheet's today when they were retired (null: not recorded). */
    @Column(name = "retire_made_on")
    private LocalDate retireMadeOn;

    /** The sheet's today when they were unretired. */
    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    protected PersonRetirement() {
    }

    public PersonRetirement(LocalDate fromDate, LocalDate toDate, LocalDate retireMadeOn, LocalDate madeOn) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.retireMadeOn = retireMadeOn;
        this.madeOn = madeOn;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDate getRetireMadeOn() {
        return retireMadeOn;
    }

    /** Whether the unretire was made while the sheet's today was on or before the date. */
    public boolean madeBy(LocalDate date) {
        return !madeOn.isAfter(date);
    }

    /** Whether the retirement was made while the sheet's today was on or before the date. */
    public boolean retiredBy(LocalDate date) {
        return retireMadeOn == null || !retireMadeOn.isAfter(date);
    }

    /** Whether a claim on the date is refused: it is after the from-date and before the to-date. */
    public boolean refuses(LocalDate date) {
        return date.isAfter(fromDate) && date.isBefore(toDate);
    }
}
