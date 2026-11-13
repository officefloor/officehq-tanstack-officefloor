package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A person on a sheet. {@code grades} are their grades (each one of one of the sheet's grades) by the date each applies
 *  from, the first from {@link #START} (the beginning of time); {@code version} moves on with each stored edit;
 *  {@code retiredFrom} is the date they are retired from (null while not retired); {@code pastRetirements}
 *  the retirements they were unretired from, oldest first; {@code holds} their holds,
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
    private final List<PastRetirement> pastRetirements = new ArrayList<>();
    private final List<Snapshot> snapshots = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();
    private LocalDate recordFrom;
    private Integer terms;
    private final NavigableMap<LocalDate, Integer> datedTerms = new TreeMap<>();
    private LocalDate termsRecordFrom;
    private boolean termsChanged;
    private String contact;
    private boolean contactChanged;
    private String origin;
    private LocalDate arrivedOn;
    private String tags;
    private Flag flag;
    private String approver;

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
    /** Their own terms (whole days) from the beginning of time, until the first dated terms; null when they have none. */
    public Integer getTerms() { return terms; }
    /** Sets their own terms from the beginning of time, noting whether they differ from the ones they had. */
    public void setTerms(Integer terms) {
        termsChanged = !java.util.Objects.equals(this.terms, terms);
        this.terms = terms;
    }
    /** Their own terms by the date each applies from, after those from the beginning of time (a null value:
     *  none of their own from that date). */
    public NavigableMap<LocalDate, Integer> getDatedTerms() { return datedTerms; }
    /** Their own terms in force on the date: the dated ones with the latest from-date on or before it, else
     *  those from the beginning of time; null when they have none then. */
    public Integer termsOn(LocalDate on) {
        Map.Entry<LocalDate, Integer> e = datedTerms.floorEntry(on);
        return e == null ? terms : e.getValue();
    }
    /** Records their own terms (null for none) from the date at the next save, keeping the others (replacing
     *  those from the same date), noting whether that changed them. */
    public void recordTerms(LocalDate from, Integer terms) {
        termsChanged = !datedTerms.containsKey(from) || !java.util.Objects.equals(datedTerms.get(from), terms);
        datedTerms.put(from, terms);
        termsRecordFrom = from;
    }
    /** The from-date of the dated terms the next save records, null when it records none. */
    public LocalDate getTermsRecordFrom() { return termsRecordFrom; }
    /** Whether the last {@link #setTerms} or {@link #recordTerms} changed their terms (or as last noted). */
    public boolean isTermsChanged() { return termsChanged; }
    /** Notes whether an edit changed their terms, overriding what the last set or record noted. */
    public void noteTermsChanged(boolean changed) { termsChanged = changed; }
    /** Their contact (trimmed, lower-cased), null when they have none. */
    public String getContact() { return contact; }
    /** Sets their contact, noting whether it differs from the one they had. */
    public void setContact(String contact) {
        contactChanged = !java.util.Objects.equals(this.contact, contact);
        this.contact = contact;
    }
    /** Whether the last {@link #setContact} changed their contact. */
    public boolean isContactChanged() { return contactChanged; }
    /** Their tags as stored (see {@link net.officefloor.hq.app.claim.Tags}), null when they have none. */
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    /** The sheet they were transferred from, null when they were not transferred here. */
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    /** The sheet's today on which a transfer brought them here, null when they were not transferred here. */
    public LocalDate getArrivedOn() { return arrivedOn; }
    public void setArrivedOn(LocalDate arrivedOn) { this.arrivedOn = arrivedOn; }
    public void setVersion(long version) { this.version = version; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    /** Their past retirements (each ended by unretiring them), oldest first. */
    public List<PastRetirement> getPastRetirements() { return pastRetirements; }
    /** Their flag, null when they were never flagged (it may be past its until-date). */
    public Flag getFlag() { return flag; }
    public void setFlag(Flag flag) { this.flag = flag; }
    /** Their approver, a user who may approve or reject their PENDING claims as a steward may (null when none). */
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    /** Whether their flag is in force on the date: only a steward may open a claim of theirs then. */
    public boolean flaggedOn(LocalDate date) { return flag != null && flag.inForceOn(date); }
    /** Their snapshots, one per SNAPSHOT written for them, oldest first. */
    public List<Snapshot> getSnapshots() { return snapshots; }
}
