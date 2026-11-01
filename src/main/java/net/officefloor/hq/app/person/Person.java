package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/** A person on a sheet. `id` is null until the person is first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    /** The person's grades by the date each is in force from; LocalDate.MIN for the first one,
     *  which applies from the beginning of time. */
    private TreeMap<LocalDate, String> grades = new TreeMap<>();
    /** The sheet's today as it was when the person was read (null: the person is new). */
    private LocalDate today;
    /** A grade this save records, from its date (null: the save records none). */
    private DatedGrade newGrade;
    private int version;
    /** The person's own terms in days from the beginning of time (null: none). */
    private Integer terms;
    /** The person's own terms by the date each is in force from (a null value: none from then). */
    private TreeMap<LocalDate, Integer> datedTerms = new TreeMap<>();
    /** Whether this save changes the person's own terms. */
    private boolean termsChanged;
    /** The person's contact (null: none). */
    private String contact;
    /** Whether this save changes the person's contact. */
    private boolean contactChanged;
    /** The person's tags, lower-cased, sorted and comma separated (null: none). */
    private String tags;
    /** The sheet the person was transferred from, and the name they had there (null: not transferred). */
    private String originSheet;
    private String originName;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    /** The person's ended retirements, in the order they ended. */
    private List<PastRetirement> pastRetirements = new ArrayList<>();
    private List<Hold> holds = new ArrayList<>();
    /** The person's flag: its reason and the date it is in force until, inclusive (null: never flagged). */
    private String flagReason;
    private LocalDate flagUntil;
    private List<String> snapshots = new ArrayList<>();
    private List<String> statementLines = new ArrayList<>();
    private List<Long> patronIds = new ArrayList<>();
    /** The person's aliases, sorted as names are compared. */
    private List<String> aliases = new ArrayList<>();
    /** Whether the person's name or an alias is used on another sheet of their sheet's tree. */
    private boolean clashing;

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, int version) {
        this.version = version;
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        if (grade != null) this.grades.put(LocalDate.MIN, grade);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /** The grade in force on the sheet's today. */
    public String getGrade() { return today == null ? grades.lastEntry().getValue() : gradeOn(today); }
    /** The grade in force on `date`: the one from the latest date on or before it, the first one
     *  applying from the beginning of time. */
    public String gradeOn(LocalDate date) {
        Map.Entry<LocalDate, String> e = grades.floorEntry(date);
        return e != null ? e.getValue() : grades.firstEntry().getValue();
    }
    /** The first grade the person was given. */
    public String getFirstGrade() { return grades.firstEntry().getValue(); }
    public TreeMap<LocalDate, String> getGrades() { return grades; }
    public void setGrades(TreeMap<LocalDate, String> grades) { this.grades = grades; }
    public void setToday(LocalDate today) { this.today = today; }
    public DatedGrade getNewGrade() { return newGrade; }
    /** Record `grade` from `from` with the next save, replacing any grade from that same date. */
    public void recordGrade(String grade, LocalDate from) {
        this.newGrade = new DatedGrade(grade, from);
        LocalDate key = grades.isEmpty() ? LocalDate.MIN : from;
        grades.put(key, grade);
    }
    /** The person's own terms in days from the beginning of time; null when they have none. */
    public Integer getTerms() { return terms; }
    public void setTerms(Integer terms) { this.terms = terms; }
    /** The person's own terms by from-date (a null value: none from then). */
    public TreeMap<LocalDate, Integer> getDatedTerms() { return datedTerms; }
    public void setDatedTerms(TreeMap<LocalDate, Integer> datedTerms) { this.datedTerms = datedTerms; }
    /** The person's own terms in force on `date`: those from the latest from-date on or before it,
     *  else those from the beginning of time (null: none). */
    public Integer termsOn(LocalDate date) {
        Map.Entry<LocalDate, Integer> e = datedTerms.floorEntry(date);
        return e != null ? e.getValue() : terms;
    }
    /** The person's own terms in force on the sheet's today (null: none). */
    public Integer getTermsInForce() { return today == null ? terms : termsOn(today); }
    public boolean isTermsChanged() { return termsChanged; }
    /** Give the person these terms (null: none) from the beginning of time with the next save,
     *  noting whether they change: from their own terms, or from the terms in force they had. */
    public void changeTerms(Integer terms) {
        this.termsChanged = !java.util.Objects.equals(this.terms, terms)
                || !java.util.Objects.equals(getTermsInForce(), terms);
        this.terms = terms;
    }
    /** Record these terms (null: none) from `from` with the next save, replacing any from that date. */
    public void recordTerms(Integer terms, LocalDate from) {
        this.termsChanged = !datedTerms.containsKey(from) || !java.util.Objects.equals(datedTerms.get(from), terms);
        datedTerms.put(from, terms);
    }
    /** The person's contact; null when they have none. */
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    /** The sheet the person was transferred from; null when they were not transferred here. */
    public String getOriginSheet() { return originSheet; }
    /** The name the person had on the sheet they were transferred from (null: not transferred). */
    public String getOriginName() { return originName; }
    public void setOrigin(String sheet, String name) { this.originSheet = sheet; this.originName = name; }
    public boolean isContactChanged() { return contactChanged; }
    /** Give the person this contact (null: none) with the next save, noting whether it changes. */
    public void changeContact(String contact) {
        this.contactChanged = !java.util.Objects.equals(this.contact, contact);
        this.contact = contact;
    }
    /** The person's tags, lower-cased, sorted and comma separated; null when they have none. */
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    /** How many times the person has been saved since added. */
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    /** The last statement's due-total; null when none was taken or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    /** The sheet's today when the last statement was taken; null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    /** The date the person is retired from; null when they are not retired. Set at all, they count
     *  as retired at once, whatever the date. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** The person's ended retirements, in the order they ended. */
    public List<PastRetirement> getPastRetirements() { return pastRetirements; }
    public void setPastRetirements(List<PastRetirement> pastRetirements) { this.pastRetirements = pastRetirements; }
    /** End the person's retirement on `on`: they are no longer retired, and it is kept as a past one. */
    public void unretire(LocalDate on) {
        pastRetirements.add(new PastRetirement(retiredFrom, on));
        retiredFrom = null;
    }
    /** Whether the sheet's today (as read) has reached the person's retirement date: from then their
     *  OPEN and PENDING claims leave the live totals, as a hold's would. */
    public boolean retiredOut() {
        return retiredFrom != null && today != null && !today.isBefore(retiredFrom);
    }
    /** Whether the person has any hold, whatever its dates. */
    public String getFlagReason() { return flagReason; }
    public LocalDate getFlagUntil() { return flagUntil; }
    public void setFlag(String reason, LocalDate until) { this.flagReason = reason; this.flagUntil = until; }
    /** Whether the person's flag is in force: the sheet's today on or before its until-date. */
    public boolean isFlagged() {
        return flagUntil != null && (today == null || !today.isAfter(flagUntil));
    }
    public boolean isHeld() { return !holds.isEmpty(); }
    /** The person's holds, in the order they were added. */
    public List<Hold> getHolds() { return holds; }
    public void setHolds(List<Hold> holds) { this.holds = holds; }
    /** The person's snapshot lines, oldest first: the day being left and their due-total then (n/a). */
    public List<String> getSnapshots() { return snapshots; }
    public void setSnapshots(List<String> snapshots) { this.snapshots = snapshots; }
    /** The person's statements still standing, in the order taken: the statement's
     *  date and its due-total (n/a). */
    public List<String> getStatementLines() { return statementLines; }
    public void setStatementLines(List<String> statementLines) { this.statementLines = statementLines; }
    /** Whether `date` falls within any of the person's holds. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }
    /** The ids of the people the person has named as patrons, in the order they were named. */
    public List<Long> getPatronIds() { return patronIds; }
    public void setPatronIds(List<Long> patronIds) { this.patronIds = patronIds; }
    /** The person's aliases, sorted as names are compared. */
    public List<String> getAliases() { return aliases; }
    public void setAliases(List<String> aliases) { this.aliases = aliases; }
    public boolean isClashing() { return clashing; }
    public void setClashing(boolean clashing) { this.clashing = clashing; }

    /** Whether a claim of the person's counts in the live totals: OPEN, PENDING and SETTLED ones do,
     *  but not a deferred one, nor an OPEN or PENDING one that is held or dated within one of the person's holds, nor
     *  one once the sheet's today has reached their retirement date, unless a statement binds it. */
    public boolean counts(Claim claim) {
        return counts(claim, claim.getStatus());
    }

    /** Whether a claim of the person's counts as it would with `status` (null: none, it does not
     *  count): by the live rules above, but with that status in place of the claim's own. */
    public boolean counts(Claim claim, ClaimStatus status) {
        if (status == null) return false;
        if (!status.counts() || claim.isDeferred()) return false;
        if (!status.isOpen() || !(claim.isHeld() || heldOn(claim.getDate()) || retiredOut())) return true;
        return claim.isStated();
    }

    /** Whether a claim of the person's counts toward claimed: as `counts`, but a hold (the claim's
     *  or the person's) does not take it out — only the due figures leave the totals while held. */
    public boolean countsClaimed(Claim claim) {
        ClaimStatus status = claim.getStatus();
        if (status == null || !status.counts() || claim.isDeferred()) return false;
        if (!status.isOpen() || !retiredOut()) return true;
        return claim.isStated();
    }

    /** Refuses a claim dated after the person's retirement date, or after the from and before the
     *  to of one of their past retirements. */
    public void refuseRetired(LocalDate claimDate) throws PersonRetiredException {
        if (retiredFrom != null && claimDate.isAfter(retiredFrom)) {
            throw new PersonRetiredException(name, retiredFrom);
        }
        for (PastRetirement past : pastRetirements) {
            if (past.covers(claimDate)) throw new PersonRetiredException(name, past);
        }
    }

    /** A grade in force from a date (null: from the beginning of time). */
    public record DatedGrade(String grade, LocalDate from) {
    }
}
