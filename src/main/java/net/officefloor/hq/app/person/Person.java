package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A person on a sheet. {@code grades} are their grades (each one of A, B or C) by the date each applies
 *  from, the first from {@link #START} (the beginning of time); {@code version} moves on with each stored edit;
 *  {@code retiredFrom} is the date they are retired from (null while not retired); {@code holds} their holds,
 *  oldest first: their OPEN claims dated within one count in no live total unless a statement bound them, nor do
 *  any of their OPEN claims once the sheet's today reaches their retirement date. */
public class Person {
    /** The from-date of a person's first grade: the beginning of time. */
    public static final LocalDate START = LocalDate.of(1, 1, 1);

    private final long id;
    private final String sheetId;
    private String name;
    private final NavigableMap<LocalDate, String> grades;
    private long version;
    private LocalDate retiredFrom;
    private final List<Hold> holds = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();
    private LocalDate recordFrom;
    private Integer terms;
    private boolean termsChanged;
    private String contact;
    private boolean contactChanged;
    private String origin;

    public Person(long id, String sheetId, String name, NavigableMap<LocalDate, String> grades, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grades = new TreeMap<>(grades);
        this.version = version;
    }

    public Person(long id, String sheetId, String name, NavigableMap<LocalDate, String> grades, long version,
            LocalDate retiredFrom, List<Hold> holds) {
        this(id, sheetId, name, grades, version);
        this.retiredFrom = retiredFrom;
        this.holds.addAll(holds);
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    /** The grade in force on the date: the one with the latest from-date on or before it. */
    public String gradeOn(LocalDate on) {
        Map.Entry<LocalDate, String> e = grades.floorEntry(on);
        return e == null ? grades.firstEntry().getValue() : e.getValue();
    }
    /** Every grade by the date it applies from ({@link #START} for the first). */
    public NavigableMap<LocalDate, String> getGrades() { return grades; }
    /** The from-date of the grade the next save records, null when it records none. */
    public LocalDate getRecordFrom() { return recordFrom; }
    public long getVersion() { return version; }
    public LocalDate getRetiredFrom() { return retiredFrom; }
    /** Retired as soon as a date is set, whatever the date. */
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether their retirement date is reached on the date: from then their OPEN (and PENDING) claims count
     *  in no live total unless a statement bound them, as a hold's would. */
    public boolean retiredOn(LocalDate date) { return retiredFrom != null && date != null && !date.isBefore(retiredFrom); }
    /** Whether the person has any hold, whatever its dates. */
    public boolean isHeld() { return !holds.isEmpty(); }
    /** Their holds, oldest first. */
    public List<Hold> getHolds() { return holds; }
    /** Whether a hold of theirs covers the date. */
    public boolean heldOn(LocalDate date) { return holds.stream().anyMatch(h -> h.covers(date)); }
    public void addHold(Hold hold) { holds.add(hold); }
    /** Their aliases: other names they are found by, sorted as names are compared. */
    public List<String> getAliases() { return aliases; }
    public void setName(String name) { this.name = name; }
    /** Records the grade from the date at the next save, keeping the others. */
    public void recordGrade(LocalDate from, String grade) {
        grades.put(from, grade);
        recordFrom = from;
    }
    /** Their own terms (whole days), null when they have none. */
    public Integer getTerms() { return terms; }
    /** Sets their own terms, noting whether they differ from the ones they had. */
    public void setTerms(Integer terms) {
        termsChanged = !java.util.Objects.equals(this.terms, terms);
        this.terms = terms;
    }
    /** Whether the last {@link #setTerms} changed their terms. */
    public boolean isTermsChanged() { return termsChanged; }
    /** Their contact (trimmed, lower-cased), null when they have none. */
    public String getContact() { return contact; }
    /** Sets their contact, noting whether it differs from the one they had. */
    public void setContact(String contact) {
        contactChanged = !java.util.Objects.equals(this.contact, contact);
        this.contact = contact;
    }
    /** Whether the last {@link #setContact} changed their contact. */
    public boolean isContactChanged() { return contactChanged; }
    /** The sheet they were transferred from, null when they were not transferred here. */
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setVersion(long version) { this.version = version; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
}
