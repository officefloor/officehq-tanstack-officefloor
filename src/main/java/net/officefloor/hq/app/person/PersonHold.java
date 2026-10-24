package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One of a person's holds: from a date on, to a date (inclusive; null: open-ended). */
@Embeddable
public class PersonHold {

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    /** The sheet's today when it was added (null: not recorded). */
    @Column(name = "made_on")
    private LocalDate madeOn;

    protected PersonHold() {
    }

    public PersonHold(LocalDate fromDate, LocalDate toDate, LocalDate madeOn) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.madeOn = madeOn;
    }

    /** Whether it was added while the sheet's today was on or before the date. */
    public boolean madeBy(LocalDate date) {
        return madeOn == null || !madeOn.isAfter(date);
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    /** Whether the date falls within the hold. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(fromDate) && (toDate == null || !date.isAfter(toDate));
    }
}
