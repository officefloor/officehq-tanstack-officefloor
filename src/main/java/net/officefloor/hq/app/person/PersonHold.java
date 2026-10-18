package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** A hold on a person from a date (none: from the beginning of time) to a date (none: open-ended), both inclusive. */
@Embeddable
public class PersonHold {

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    protected PersonHold() {
    }

    public PersonHold(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    /** Whether the date falls within the hold. */
    public boolean covers(LocalDate date) {
        return (fromDate == null || !date.isBefore(fromDate)) && (toDate == null || !date.isAfter(toDate));
    }
}
