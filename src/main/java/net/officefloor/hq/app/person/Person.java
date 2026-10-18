package net.officefloor.hq.app.person;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** A person on a sheet, with a name and a dated grade (A, B or C); once retired, the date retired from; their holds; the people named as their patrons. */
@Entity
@Table(name = "person")
public class Person {

    private static final Comparator<PersonGrade> BY_FROM =
            Comparator.comparing(PersonGrade::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "name", nullable = false)
    private String name;

    /** The grades the person has held, each from its from-date (the first from the beginning of time). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_grade", joinColumns = @JoinColumn(name = "person_id"))
    private List<PersonGrade> grades = new ArrayList<>();

    /** The grade recorded by this save, for its audit line. */
    @Transient
    private String savedGrade;

    /** Raised by each save; an edit made against an older version is refused. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** None while active; a claim dated after it is refused. */
    @Column(name = "retired_from")
    private LocalDate retiredFrom;

    /** The person's holds: their OPEN and PENDING claims dated within one count in no live total; their
     *  other claims still do. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_hold", joinColumns = @JoinColumn(name = "person_id"))
    private List<PersonHold> holds = new ArrayList<>();

    /** The hold added by this save, for its audit line. */
    @Transient
    private PersonHold addedHold;

    /** The ids of the other people on the sheet named as this person's patrons. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_patron", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "patron_id", nullable = false)
    private Set<Long> patronIds = new LinkedHashSet<>();

    protected Person() {
    }

    public Person(String sheetId) {
        this.sheetId = sheetId;
    }

    public Long getId() {
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

    /** The grades, by from-date (none, the beginning of time, first). */
    public List<PersonGrade> getGrades() {
        return grades.stream().sorted(BY_FROM).toList();
    }

    /** The grade in force on the date: the latest from-date on or before it; null before them all. */
    public String gradeOn(LocalDate date) {
        return grades.stream().filter((g) -> g.getFromDate() == null || !g.getFromDate().isAfter(date))
                .max(BY_FROM).map(PersonGrade::getGrade).orElse(null);
    }

    /** Records the grade from the date on, replacing any grade recorded for that date; the person's
     *  first grade applies from the beginning of time whatever the date. */
    public void recordGrade(String grade, LocalDate from) {
        savedGrade = grade;
        if (grades.isEmpty()) {
            grades.add(new PersonGrade(null, grade));
            return;
        }
        if (grade.equals(gradeOn(from))) {
            return; // already in force from then on
        }
        grades.removeIf((g) -> from.equals(g.getFromDate()));
        grades.add(new PersonGrade(from, grade));
    }

    /** The grade this save recorded (the audit line's grade). */
    public String getSavedGrade() {
        return savedGrade;
    }

    public long getVersion() {
        return version;
    }

    public LocalDate getRetiredFrom() {
        return retiredFrom;
    }

    public void setRetiredFrom(LocalDate retiredFrom) {
        this.retiredFrom = retiredFrom;
    }

    /** The holds, by from-date (none, the beginning of time, first). */
    public List<PersonHold> getHolds() {
        return holds.stream().sorted(Comparator.comparing(PersonHold::getFromDate,
                Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
    }

    /** Whether the person has any hold. */
    public boolean isHeld() {
        return !holds.isEmpty();
    }

    /** Whether the date falls within one of the person's holds. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch((h) -> h.covers(date));
    }

    public void addHold(PersonHold hold) {
        holds.add(hold);
        addedHold = hold;
    }

    /** The hold this save added (the audit line's from-date). */
    public PersonHold getAddedHold() {
        return addedHold;
    }

    public Set<Long> getPatronIds() {
        return patronIds;
    }
}
