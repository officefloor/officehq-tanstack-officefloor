package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A person on a sheet, as the store holds it. {@code id} is null until first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    private String grade;
    private Integer terms;
    private String contact;
    private String tags;
    private long version;
    private int statementCount;
    private String statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    private LocalDate addedOn;
    private String originSheet;
    private String flagReason;
    private LocalDate flagUntil;
    private String approver;
    private final List<Hold> holds = new ArrayList<>();
    private final List<Long> patronIds = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();
    private final List<PastRetirement> pastRetirements = new ArrayList<>();
    private final List<String> snapshots = new ArrayList<>();
    private final List<String> statementLines = new ArrayList<>();
    private final TreeMap<LocalDate, String> datedGrades = new TreeMap<>();
    private final TreeMap<LocalDate, Integer> datedTerms = new TreeMap<>();

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /** The first grade they were given: in force from the beginning of time, until their first dated grade. */
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** Their own terms in days from the beginning of time, until their first dated terms; null when they have none. */
    public Integer getTerms() { return terms; }
    public void setTerms(Integer terms) { this.terms = terms; }
    /** The terms recorded from a date, by from-date (null: none): each in force from it until the next. */
    public Map<LocalDate, Integer> getDatedTerms() { return datedTerms; }
    /** Record the terms (null: none) from this date, replacing any recorded from the same date. */
    public void setTermsFrom(Integer terms, LocalDate from) { datedTerms.put(from, terms); }
    /**
     * Their own terms in force on the date, ahead of their grade's and the sheet's: the latest dated
     * ones on or before it, else those from the beginning of time; null when they have none.
     */
    public Integer termsOn(LocalDate date) {
        Map.Entry<LocalDate, Integer> dated = datedTerms.floorEntry(date);
        return dated == null ? terms : dated.getValue();
    }
    /** Their contact, trimmed and lower-cased; null when they have none. */
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    /** Their tags: lower-cased, without repeats, sorted and comma separated; null when they have none. */
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    /** Their tags, sorted; empty when they have none. */
    public List<String> tagList() { return tags == null ? List.of() : List.of(tags.split(",")); }
    /** The grades recorded from a date, by from-date: each in force from it until the next. */
    public Map<LocalDate, String> getDatedGrades() { return datedGrades; }
    /** Record the grade from this date, replacing any recorded from the same date. */
    public void setGradeFrom(String grade, LocalDate from) { datedGrades.put(from, grade); }
    /** The grade in force on the date: the latest dated one on or before it, else the first grade. */
    public String gradeOn(LocalDate date) {
        Map.Entry<LocalDate, String> dated = datedGrades.floorEntry(date);
        return dated == null ? grade : dated.getValue();
    }
    /** Raised by one on each save; an edit names the version it was made from. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    /** How many statements have been taken for them. */
    public int getStatementCount() { return statementCount; }
    /** The last statement's due-total (n/a when it had none); null before the first. */
    public String getStatedTotal() { return statedTotal; }
    /** The sheet's today when the last statement was taken; null before the first. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatements(int count, String statedTotal, LocalDate statementDate) {
        this.statementCount = count;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }
    /** The sheet's today when they were added; null when it was not kept (from the beginning of time). */
    public LocalDate getAddedOn() { return addedOn; }
    public void setAddedOn(LocalDate addedOn) { this.addedOn = addedOn; }
    /** The sheet they were transferred from (their latest transfer); null when never transferred. */
    public String getOriginSheet() { return originSheet; }
    public void setOriginSheet(String originSheet) { this.originSheet = originSheet; }
    /** Their flag's reason; null when never flagged. */
    public String getFlagReason() { return flagReason; }
    /** The date their flag is in force until, inclusive; null when never flagged. */
    public LocalDate getFlagUntil() { return flagUntil; }
    public void setFlag(String reason, LocalDate until) {
        this.flagReason = reason;
        this.flagUntil = until;
    }
    /** The user who may approve or reject their PENDING claims as a steward may; null when none. */
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    /** Whether {@code user} is their approver. */
    public boolean approvedBy(String user) { return approver != null && !approver.isEmpty() && approver.equals(user); }
    /** Whether their flag is in force on {@code today}: only a steward may open a claim of theirs. */
    public boolean flaggedOn(LocalDate today) {
        return flagReason != null && flagUntil != null && today != null && !today.isAfter(flagUntil);
    }
    /** Whether their flag stops {@code role} opening a claim of theirs on {@code today}. */
    public boolean flagRefuses(String role, LocalDate today) {
        return flaggedOn(today) && !net.officefloor.hq.app.role.Role.STEWARD.equals(role);
    }
    /** Whether they had been added by the end of {@code day}. */
    public boolean addedBy(LocalDate day) { return addedOn == null || !addedOn.isAfter(day); }
    /** The date they are retired from; null when not retired. Set at all, they count as retired. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** The retirements they were unretired from, oldest first. */
    public List<PastRetirement> getPastRetirements() { return pastRetirements; }
    /** Their snapshot lines, oldest first, each as "<the day being left> <due-total>". */
    public List<String> getSnapshots() { return snapshots; }
    /** Their statements still standing, oldest first (by date), each as "<statement date> <due-total>". */
    public List<String> getStatementLines() { return statementLines; }
    /** Their holds, in the order added: each from a date to an optional to-date. */
    public List<Hold> getHolds() { return holds; }
    /** Whether they have any hold at all. */
    public boolean isHeld() { return !holds.isEmpty(); }
    /** Whether the date is within one of their holds (their claims still count in every total). */
    public boolean heldOn(LocalDate date) { return holds.stream().anyMatch((h) -> h.covers(date)); }
    /** Whether their retirement date is reached on {@code today}: retired from it or an earlier date. */
    public boolean retiredOn(LocalDate today) { return retiredFrom != null && today != null && !today.isBefore(retiredFrom); }
    /**
     * Whether an OPEN or PENDING claim of theirs on the date, not bound by a statement, counts in no
     * total on {@code today}: their retirement date is reached. A hold does not take it out; it only
     * takes away their pool cut.
     */
    public boolean heldOn(LocalDate date, LocalDate today) { return retiredOn(today); }
    /** The ids of the people they name as patrons, in the order named. */
    public List<Long> getPatronIds() { return patronIds; }
    /** Their aliases, trimmed, in the order given: like names, none equal (ignoring case) any name or alias on the sheet. */
    public List<String> getAliases() { return aliases; }
    /**
     * Whether a claim on this date is refused: they are retired from an earlier date, or it is after
     * the from and before the to of a past retirement.
     */
    public boolean retiredBefore(LocalDate date) {
        return (retiredFrom != null && date.isAfter(retiredFrom)) || pastRetirements.stream().anyMatch((r) -> r.refuses(date));
    }
}
