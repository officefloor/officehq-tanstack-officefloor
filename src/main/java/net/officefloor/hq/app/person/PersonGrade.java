package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A grade recorded for a person from a date on; the grade they were first given is the person's own. */
@Entity
@Table(name = "person_grade")
public class PersonGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(nullable = false)
    private String grade;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /** The sheet's today when it was last recorded (null: not recorded). */
    @Column(name = "made_on")
    private LocalDate madeOn;

    protected PersonGrade() {
    }

    public PersonGrade(String sheetId, Long personId, String grade, LocalDate fromDate) {
        this.sheetId = sheetId;
        this.personId = personId;
        this.grade = grade;
        this.fromDate = fromDate;
    }

    public void setMadeOn(LocalDate madeOn) {
        this.madeOn = madeOn;
    }

    /** Whether it was recorded while the sheet's today was on or before the date. */
    public boolean madeBy(LocalDate date) {
        return madeOn == null || !madeOn.isAfter(date);
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public Long getPersonId() {
        return personId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }
}
