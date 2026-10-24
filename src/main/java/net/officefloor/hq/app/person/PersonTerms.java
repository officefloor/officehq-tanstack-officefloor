package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** A person's own terms from a date on (null terms: none, so their grade's or the sheet's apply). */
@Embeddable
public class PersonTerms {

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "terms")
    private Integer terms;

    protected PersonTerms() {
    }

    public PersonTerms(LocalDate fromDate, Integer terms) {
        this.fromDate = fromDate;
        this.terms = terms;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public Integer getTerms() {
        return terms;
    }
}
