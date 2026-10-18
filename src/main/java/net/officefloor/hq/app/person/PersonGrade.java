package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** A grade a person holds from a date on (none: from the beginning of time). */
@Embeddable
public class PersonGrade {

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "grade", nullable = false)
    private String grade;

    protected PersonGrade() {
    }

    public PersonGrade(LocalDate fromDate, String grade) {
        this.fromDate = fromDate;
        this.grade = grade;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public String getGrade() {
        return grade;
    }
}
