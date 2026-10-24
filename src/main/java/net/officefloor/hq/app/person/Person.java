package net.officefloor.hq.app.person;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A person on a sheet: a name and a grade (A, B or C), the date they are retired from (null: not retired),
 *  and their holds over time. */
@Entity
@Table(name = "person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String grade;

    /** Their own terms in days (null: none): a claim of theirs is due by its date plus these, ahead of their
     *  grade's terms and the sheet's. */
    private Integer terms;

    /** Set by an edit that changed their terms, for its audit record; not stored. */
    @Transient
    private boolean termsChanged;

    /** Their contact, trimmed and lower-cased (null: none); no two people on a sheet share one. */
    private String contact;

    /** Set by an edit that changed their contact, for its audit record; not stored. */
    @Transient
    private boolean contactChanged;

    /** Their tags, trimmed, lower-cased, without repeats, sorted and comma separated (null: none); the tag totals
     *  count each claim of theirs for these as well as for the claim's own. */
    private String tags;

    /** Their place in the sheet's list, lowest first (null: their id, which puts a person added later at the end). */
    private Long position;

    /** Raised on every save; an edit naming an older version is refused. */
    @Version
    @Column(nullable = false)
    private long version;

    /** Set: the person counts as retired at once, whatever the date; claims dated after it are refused. From the day
     *  it is reached (the sheet's today on or after it) their unbound OPEN and PENDING claims leave the live totals. */
    @Column(name = "retired_from")
    private LocalDate retiredFrom;

    /** Their holds, earliest from-date first: their OPEN and PENDING claims dated within one count in no live
     *  total, unless a statement binds them; their other claims still count. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_hold", joinColumns = @JoinColumn(name = "person_id"))
    @OrderBy("fromDate ASC, toDate ASC")
    private List<PersonHold> holds = new ArrayList<>();

    /** Their past retirements, each ended by an unretire, earliest from-date first: a claim dated after the from and
     *  before the to of one is refused, as one after a retirement is. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_retirement", joinColumns = @JoinColumn(name = "person_id"))
    @OrderBy("fromDate ASC, toDate ASC")
    private List<PersonRetirement> retirements = new ArrayList<>();

    /** The sheet's today when they were added (null: not recorded); a statement as of an earlier date is refused. */
    @Column(name = "added_on")
    private LocalDate addedOn;

    /** The sheet's today when they were retired (null: not recorded, or not retired). */
    @Column(name = "retired_made_on")
    private LocalDate retiredMadeOn;

    /** The sheet they were transferred from (null: they were not transferred here). */
    @Column(name = "origin_sheet")
    private String originSheet;

    protected Person() {
    }

    public Person(String sheetId, String name, String grade) {
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() {
        return id;
    }

    /** Their place in the sheet's list, lowest first: their position, or their id with none set. */
    public long getPlace() {
        return position != null ? position : id;
    }

    /** Swaps their place in the list with another person's. */
    public void swapPlace(Person other) {
        long mine = getPlace();
        position = other.getPlace();
        other.position = mine;
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

    public Integer getTerms() {
        return terms;
    }

    /** Sets their terms, noting whether they differ from the terms they had. */
    public void setTerms(Integer terms) {
        termsChanged = !java.util.Objects.equals(this.terms, terms);
        this.terms = terms;
    }

    public boolean isTermsChanged() {
        return termsChanged;
    }

    public String getOriginSheet() {
        return originSheet;
    }

    public void setOriginSheet(String originSheet) {
        this.originSheet = originSheet;
    }

    public String getContact() {
        return contact;
    }

    /** Sets their contact, noting whether it differs from the one they had. */
    public void setContact(String contact) {
        contactChanged = !java.util.Objects.equals(this.contact, contact);
        this.contact = contact;
    }

    public boolean isContactChanged() {
        return contactChanged;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public long getVersion() {
        return version;
    }

    public LocalDate getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(LocalDate addedOn) {
        this.addedOn = addedOn;
    }

    /** Retires them from a date, the change made on the sheet's today given. */
    public void retire(LocalDate retiredFrom, LocalDate madeOn) {
        this.retiredFrom = retiredFrom;
        this.retiredMadeOn = madeOn;
    }

    /** Unretires them on the sheet's today given: their retirement is kept as a past one, from its date to that day. */
    public void unretire(LocalDate on) {
        retirements.add(new PersonRetirement(retiredFrom, on, retiredMadeOn, on));
        this.retiredFrom = null;
        this.retiredMadeOn = null;
    }

    /** A copy of them as they stood at the end of a date: only the holds and retirement made while the sheet's
     *  today was on or before it. Only worked out, never saved. */
    public Person asOf(LocalDate date) {
        Person then = new Person(sheetId, name, grade);
        then.id = id;
        then.position = position;
        then.terms = terms;
        then.contact = contact;
        then.tags = tags;
        then.addedOn = addedOn;
        if (retiredMadeOn == null || !retiredMadeOn.isAfter(date)) {
            then.retiredFrom = retiredFrom;
            then.retiredMadeOn = retiredMadeOn;
        }
        for (PersonRetirement r : retirements) {
            if (r.madeBy(date)) {
                then.retirements.add(r);
            } else if (r.retiredBy(date)) {
                then.retiredFrom = r.getFromDate();
                then.retiredMadeOn = r.getRetireMadeOn();
            }
        }
        holds.stream().filter(h -> h.madeBy(date)).forEach(then.holds::add);
        return then;
    }

    public LocalDate getRetiredFrom() {
        return retiredFrom;
    }

    public List<PersonRetirement> getRetirements() {
        return retirements;
    }

    public List<PersonHold> getHolds() {
        return holds;
    }

    public void addHold(PersonHold hold) {
        holds.add(hold);
    }

    /** Whether they have any hold. */
    public boolean isHeld() {
        return !holds.isEmpty();
    }

    /** Whether a hold of theirs covers the date. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }

    /** Whether their retirement date is reached on the day: it is on or before it. */
    public boolean retiredBy(LocalDate today) {
        return retiredFrom != null && today != null && !today.isBefore(retiredFrom);
    }

    /** Whether their OPEN and PENDING claims on the date leave the live totals on the day: a hold of theirs covers
     *  the date, or their retirement date is reached. */
    public boolean leftOn(LocalDate date, LocalDate today) {
        return heldOn(date) || retiredBy(today);
    }

    /** Whether a claim on this date is refused: it is after their retirement date, or after the from and before the
     *  to of a past retirement. */
    public boolean retiredOn(LocalDate date) {
        return (retiredFrom != null && date.isAfter(retiredFrom))
                || retirements.stream().anyMatch(r -> r.refuses(date));
    }
}
