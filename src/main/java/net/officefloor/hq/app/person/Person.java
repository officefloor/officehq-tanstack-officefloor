package net.officefloor.hq.app.person;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

public class Person {

    /** The from-date standing for the beginning of time (stored as a null from-date). */
    public static final LocalDate BEGINNING = LocalDate.MIN;

    private final long id;
    private final String sheetId;
    private String name;
    /** The person's grades by the date each is in force from; {@link #BEGINNING} is the beginning of time. */
    private final java.util.TreeMap<LocalDate, String> grades = new java.util.TreeMap<>();
    /** The sheet's today when the person was loaded: the grade in force on it is the one shown. */
    private LocalDate today;
    /** A grade recorded by this edit and not yet written: its from-date and grade. */
    private java.util.Map.Entry<LocalDate, String> recorded;
    private int version;
    /** The person's own terms in days; null when they have none (their grade's or the sheet's apply). */
    private Integer terms;
    /** Whether this edit changed the person's terms from those loaded. */
    private boolean termsChanged;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    /** The person's holds, in the order they were added. */
    private java.util.List<Hold> holds = new java.util.ArrayList<>();
    private java.util.List<Long> patronIds = new java.util.ArrayList<>();
    private java.util.List<String> patronNames = new java.util.ArrayList<>();
    /** The person's aliases, sorted as names are compared. */
    private java.util.List<String> aliases = new java.util.ArrayList<>();
    /** Whether the person's name or an alias is used by someone else on a sheet of the sheet's tree. */
    private boolean clashing;

    public Person(long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(long id, String sheetId, String name, String grade, int version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grades.put(BEGINNING, grade);
        this.version = version;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    /** The grade in force on the sheet's today. */
    public String getGrade() { return gradeOn(today); }
    /** The grade in force on a date: the one with the latest from-date on or before it; with no date, the first. */
    public String gradeOn(LocalDate date) {
        java.util.Map.Entry<LocalDate, String> found = date == null ? null : grades.floorEntry(date);
        return found == null ? grades.firstEntry().getValue() : found.getValue();
    }
    /** The person's grades by from-date ({@link #BEGINNING} for the first), earliest first. */
    public java.util.SortedMap<LocalDate, String> getGrades() { return java.util.Collections.unmodifiableSortedMap(grades); }
    public void setGrades(java.util.Map<LocalDate, String> loaded) {
        if (!loaded.isEmpty()) {
            grades.clear();
            grades.putAll(loaded);
        }
    }
    /** Records the grade from the date (replacing one recorded from that same date), to be written on save. */
    public void recordGrade(LocalDate from, String grade) {
        grades.put(from, grade);
        recorded = java.util.Map.entry(from, grade);
    }
    /** The grade this edit recorded and has not written yet; null when none. */
    public java.util.Map.Entry<LocalDate, String> getRecordedGrade() { return recorded; }
    public void clearRecordedGrade() { recorded = null; }
    public LocalDate getToday() { return today; }
    public void setToday(LocalDate today) { this.today = today; }
    public int getVersion() { return version; }
    /** The person's own terms in days; null when none. */
    public Integer getTerms() { return terms; }
    public void setTerms(Integer terms) { this.terms = terms; }
    /** Sets the terms the form gave, noting whether they differ from those the person had. */
    public void editTerms(Integer terms) {
        termsChanged = !java.util.Objects.equals(this.terms, terms);
        this.terms = terms;
    }
    public boolean isTermsChanged() { return termsChanged; }
    /** The person's contact, trimmed and lower-cased; null when none. */
    private String contact;
    /** Whether this edit changed the person's contact from the one loaded. */
    private boolean contactChanged;
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    /** Sets the contact the form gave, noting whether it differs from the one the person had. */
    public void editContact(String contact) {
        contactChanged = !java.util.Objects.equals(this.contact, contact);
        this.contact = contact;
    }
    public boolean isContactChanged() { return contactChanged; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    /** The last statement's due-total; null when none was taken or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    /** The sheet's today when the last statement was taken; null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    /** The date the person is retired from; null when not retired. Set means retired, whatever the date. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether a claim may be dated on this date: never after the person's retirement date. */
    public boolean allowsClaimOn(LocalDate date) { return retiredFrom == null || !date.isAfter(retiredFrom); }
    /** A hold from a date to an optional to-date (null: open-ended), both days included. */
    public record Hold(LocalDate from, LocalDate to) {
        public boolean covers(LocalDate date) {
            return date != null && !date.isBefore(from) && (to == null || !date.isAfter(to));
        }
    }
    /** Whether the person has any hold. */
    public boolean isHeld() { return !holds.isEmpty(); }
    /** The person's holds, in the order they were added. */
    public java.util.List<Hold> getHolds() { return holds; }
    public void setHolds(java.util.List<Hold> holds) { this.holds = holds; }
    /** Whether a hold of the person's covers the date. */
    public boolean heldOn(LocalDate date) { return holds.stream().anyMatch(h -> h.covers(date)); }
    /** Whether the claim counts in the live totals: OPEN, PENDING or SETTLED, but not OPEN or PENDING
     *  when dated within one of the person's holds, unless a statement binds it. */
    public boolean countsLive(Claim claim) {
        return claim.getStatus().counts()
                && !(claim.getStatus().isOpen() && !claim.isBound() && heldOn(claim.getDate()));
    }
    /** Whether the person's retirement date has been reached on the date (the sheet's today on or after it). */
    public boolean retiredOn(LocalDate today) {
        return retiredFrom != null && today != null && !today.isBefore(retiredFrom);
    }
    /** Whether the claim counts in the live totals on the sheet's today: as {@link #countsLive(Claim)}, not deferred,
     *  and, once the person's retirement date is reached, not OPEN or PENDING unless a statement binds it. */
    public boolean countsLive(Claim claim, LocalDate today) {
        return countsLive(claim) && !claim.isDeferredOn(today)
                && !(claim.getStatus().isOpen() && !claim.isBound() && retiredOn(today));
    }
    /** Whether the claim counts in a period ending on the date: by the status it had at the end of that day (see
     *  {@link Claim#statusAt}), and, as in the live totals on the sheet's today, not deferred and not open, unbound and
     *  dated within one of the person's holds. */
    public boolean countsAtEnd(Claim claim, LocalDate end, LocalDate today) {
        ClaimStatus then = claim.statusAt(end);
        return then != null && then.counts() && !claim.isDeferredOn(today)
                && !(claim.getStatus().isOpen() && !claim.isBound() && heldOn(claim.getDate()));
    }
    /** The ids of the people the person has named as patrons, in the order they were named. */
    public java.util.List<Long> getPatronIds() { return patronIds; }
    /** The names of the person's patrons, sorted. */
    public java.util.List<String> getPatronNames() { return patronNames; }
    public void setPatrons(java.util.List<Long> ids, java.util.List<String> names) {
        this.patronIds = ids;
        this.patronNames = names;
    }
    /** The person's aliases, sorted as names are compared. */
    public java.util.List<String> getAliases() { return aliases; }
    public void setAliases(java.util.List<String> aliases) { this.aliases = aliases; }
    /** Whether the person's name or an alias is used by someone else on a sheet of the sheet's tree. */
    public boolean isClashing() { return clashing; }
    public void setClashing(boolean clashing) { this.clashing = clashing; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public void setName(String name) { this.name = name; }
    /** The sheet's today when the person was added; null when not known. */
    private LocalDate addedOn;
    public LocalDate getAddedOn() { return addedOn; }
    public void setAddedOn(LocalDate addedOn) { this.addedOn = addedOn; }
    public void setVersion(int version) { this.version = version; }
}
