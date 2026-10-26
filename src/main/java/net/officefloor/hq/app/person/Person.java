package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private List<GradeFrom> grades = List.of();
    private long version;
    private Statements statements = Statements.NONE;
    private LocalDate retiredFrom;
    private List<HoldSpan> holds = List.of();
    private List<String> patrons = List.of();
    private List<String> aliases = List.of();
    private Integer terms;
    private String contact;

    private String origin;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() {
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

    /** The grade in force on the sheet's today. */
    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    /** The grades the person was given, each from its from-date (the first from the beginning of time). */
    public List<GradeFrom> getGrades() {
        return grades;
    }

    public void setGrades(List<GradeFrom> grades) {
        this.grades = grades;
    }

    /** The grade in force on the date: the one with the latest from-date on or before it, none counting as the earliest. */
    public String gradeOn(LocalDate on) {
        GradeFrom found = null;
        for (GradeFrom g : grades) {
            if (g.from() != null && g.from().isAfter(on)) {
                continue;
            }
            if (found == null || (g.from() != null && (found.from() == null || g.from().isAfter(found.from())))) {
                found = g;
            }
        }
        return found == null ? grade : found.grade();
    }

    /** The person's own terms in days; null when they have none. */
    public Integer getTerms() {
        return terms;
    }

    public void setTerms(Integer terms) {
        this.terms = terms;
    }

    /** The person's contact, trimmed and lower-cased; null when they have none. */
    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    /** The sheet the person was transferred from; null when they were not transferred in. */
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /** The person's statements: how many, and the last one's due-total and date. */
    public Statements getStatements() {
        return statements;
    }

    public void setStatements(Statements statements) {
        this.statements = statements;
    }

    /** The date the person is retired from; null when not retired. Set at all, the person counts as retired. */
    public LocalDate getRetiredFrom() {
        return retiredFrom;
    }

    public void setRetiredFrom(LocalDate retiredFrom) {
        this.retiredFrom = retiredFrom;
    }

    public boolean isRetired() {
        return retiredFrom != null;
    }

    /** Whether the person's retirement date is reached on the date: from then their OPEN and PENDING claims, unless a
     *  statement binds them, count in no live total, as a hold's would. */
    public boolean retiredOn(LocalDate on) {
        return retiredFrom != null && !on.isBefore(retiredFrom);
    }

    /** Whether the person has any hold. */
    public boolean isHeld() {
        return !holds.isEmpty();
    }

    /** The person's holds, in the order they were added. */
    public List<HoldSpan> getHolds() {
        return holds;
    }

    public void setHolds(List<HoldSpan> holds) {
        this.holds = holds;
    }

    /** Whether a hold covers the date: the person's OPEN and PENDING claims dated then, unless a statement binds them,
     *  count in no live total. */
    public boolean heldOn(LocalDate on) {
        return holds.stream().anyMatch(h -> h.covers(on));
    }

    /** The names of the person's patrons, sorted. */
    public List<String> getPatrons() {
        return patrons.stream().sorted().toList();
    }

    /** The names of the person's patrons, in the order they were named. */
    public List<String> getPatronsNamed() {
        return patrons;
    }

    /** Sets the names of the person's patrons, in the order they were named. */
    public void setPatrons(List<String> patrons) {
        this.patrons = patrons;
    }

    /**
     * The person's patrons with their shares, sorted by name: 100.00 split equally in the order they were named, each
     * share rounded half to even to two decimals, the remainder (100.00 less the shares added up) to the first named.
     */
    public List<PatronShare> getPatronShares() {
        if (patrons.isEmpty()) {
            return List.of();
        }
        BigDecimal hundred = new BigDecimal("100.00");
        BigDecimal each = hundred.divide(BigDecimal.valueOf(patrons.size()), 2, RoundingMode.HALF_EVEN);
        BigDecimal first = hundred.subtract(each.multiply(BigDecimal.valueOf(patrons.size() - 1)));
        List<PatronShare> shares = new ArrayList<>();
        for (int i = 0; i < patrons.size(); i++) {
            shares.add(new PatronShare(patrons.get(i), i == 0 ? first : each));
        }
        shares.sort(Comparator.comparing(PatronShare::name));
        return shares;
    }

    private boolean clashing;

    /** Whether the person's name or an alias of theirs is used, as a name or alias, on another sheet of their tree. */
    public boolean isClashing() {
        return clashing;
    }

    public void setClashing(boolean clashing) {
        this.clashing = clashing;
    }

    /** The person's aliases, sorted as names are compared. */
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
}
