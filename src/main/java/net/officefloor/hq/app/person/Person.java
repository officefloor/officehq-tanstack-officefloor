package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.setting.DatedSettings;

/**
 * A person on a sheet. {@code id} is null until the person is first saved; {@code version} counts its saves.
 * {@code statementCount} counts the statements taken; {@code statedTotal} is the last one's due-total
 * (n/a when it had none) and {@code statementDate} the sheet's today when it was taken, both null before the first.
 * {@code retiredFrom} is the date the person is retired from, or null when they are not retired. {@code holds} are
 * the person's holds, oldest first: their open claims dated within one, and not bound by a statement, count in no
 * live total. {@code grades} are the grades
 * recorded for the person, each from its from-date; {@code grade} is the one in force on the sheet's today.
 * {@code termsFrom} are the person's own terms recorded, each from its from-date (one from the beginning of time);
 * {@code terms} are those in force on the sheet's today, in days, or null when they have none then. {@code contact}
 * is the person's contact, trimmed and lower-cased, or null when they have none. {@code pastRetirements} are the
 * retirements they were unretired from, oldest first.
 */
public record Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
        String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds, List<GradeFrom> grades,
        Integer terms, String contact, List<PastRetirement> pastRetirements, List<TermsFrom> termsFrom) {

    /** A person whose only terms of their own apply from the beginning of time. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds,
            List<GradeFrom> grades, Integer terms, String contact, List<PastRetirement> pastRetirements) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                grades, terms, contact, pastRetirements, List.of(new TermsFrom(terms, null)));
    }

    /** A person never unretired. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds,
            List<GradeFrom> grades, Integer terms, String contact) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                grades, terms, contact, List.of());
    }

    /** A person without a contact. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds,
            List<GradeFrom> grades, Integer terms) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                grades, terms, null);
    }

    /** A person without terms of their own. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds,
            List<GradeFrom> grades) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                grades, null);
    }

    /** A person whose only grade applies from the beginning of time. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                List.of(new GradeFrom(grade, null)));
    }

    /** The grade in force on the date: the one recorded with the latest from-date on or before it. */
    public String gradeOn(LocalDate date) {
        return gradeOn(grades, date, grade);
    }

    static String gradeOn(List<GradeFrom> grades, LocalDate date, String otherwise) {
        return grades.stream().filter((g) -> g.from() == null || !g.from().isAfter(date))
                .max(Comparator.comparing(GradeFrom::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(GradeFrom::grade).orElse(otherwise);
    }

    /** The same person with another name. */
    public Person withName(String nextName) {
        return new Person(id, sheetId, nextName, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, terms, contact, pastRetirements, termsFrom);
    }

    /** The same person with this contact (null: none). */
    public Person withContact(String nextContact) {
        return new Person(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, terms, nextContact, pastRetirements, termsFrom);
    }

    /** The same person with these terms of their own (null: none) from the beginning of time, and no others. */
    public Person withTerms(Integer nextTerms) {
        return new Person(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, nextTerms, contact, pastRetirements);
    }

    /**
     * The same person with these terms of their own (null: none) recorded from the date (null: the beginning of
     * time), replacing any recorded from that same date and keeping the others; {@code terms} become those in force
     * on {@code today}.
     */
    public Person withTerms(Integer nextTerms, LocalDate from, LocalDate today) {
        List<TermsFrom> next = new ArrayList<>();
        for (TermsFrom t : termsFrom) {
            if (!Objects.equals(t.from(), from)) {
                next.add(t);
            }
        }
        next.add(new TermsFrom(nextTerms, from));
        next.sort(Comparator.comparing(TermsFrom::from, Comparator.nullsFirst(Comparator.naturalOrder())));
        return new Person(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, ownTermsOn(next, today), contact, pastRetirements, List.copyOf(next));
    }

    /** The person's own terms in force on the date: those recorded with the latest from-date on or before it. */
    public Integer ownTermsOn(LocalDate date) {
        return ownTermsOn(termsFrom, date);
    }

    static Integer ownTermsOn(List<TermsFrom> recorded, LocalDate date) {
        return recorded.stream().filter((t) -> t.from() == null || !t.from().isAfter(date))
                .max(Comparator.comparing(TermsFrom::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(TermsFrom::terms).orElse(null);
    }

    /** The terms of their own the person had from the beginning of time (null: none). */
    public Integer firstTerms() {
        return termsFrom.stream().filter((t) -> t.from() == null).findFirst().map(TermsFrom::terms).orElse(null);
    }

    /**
     * The terms a claim of the person's dated on the date is due by: their own in force on the date, else those of
     * their grade in force on the date, else the sheet's in force on it.
     */
    public int termsOn(DatedSettings settings, LocalDate date) {
        Integer own = ownTermsOn(date);
        return own != null ? own : settings.termsFor(gradeOn(date), date);
    }

    /**
     * The same person with another name and this grade recorded from the date (replacing any recorded from that
     * same date), keeping the others; {@code grade} becomes the one in force on {@code today}.
     */
    public Person withNameAndGrade(String nextName, String nextGrade, LocalDate from, LocalDate today) {
        List<GradeFrom> next = new ArrayList<>();
        for (GradeFrom g : grades) {
            if (g.from() == null ? from != null : !g.from().equals(from)) {
                next.add(g);
            }
        }
        next.add(new GradeFrom(nextGrade, from));
        return new Person(id, sheetId, nextName, gradeOn(next, today, nextGrade), version, statementCount,
                statedTotal, statementDate, retiredFrom, holds, List.copyOf(next), terms, contact, pastRetirements,
                termsFrom);
    }

    /** Whether the person has any hold, whatever its dates. */
    public boolean held() {
        return !holds.isEmpty();
    }

    /**
     * Whether the claim counts toward this person's live totals on the given today: a claim deferred then does not,
     * nor, once their retirement date is reached, do their open claims, unless a statement binds them. A hold (theirs
     * or the claim's own) does not take a claim out.
     */
    public boolean counts(Claim claim, LocalDate today) {
        return !claim.deferred(today) && claim.counts(!claim.bound() && heldOn(claim.date(), today));
    }

    /**
     * Whether the claim counts toward this person's claimed on the given today: as {@link #counts}, but a hold (theirs
     * or the claim's own) does not take it out; once their retirement date is reached, none of theirs does but those a
     * statement binds.
     */
    public boolean countsClaimed(Claim claim, LocalDate today) {
        return (!retiredOn(today) || claim.bound()) && !claim.deferred(today) && claim.counts(false);
    }

    /**
     * Whether the person's open claims on this date are left out of the live totals on the given today: their
     * retirement date has been reached. A hold does not leave them out (it only takes away their pool cut).
     */
    public boolean heldOn(LocalDate date, LocalDate today) {
        return retiredOn(today);
    }

    /** Whether the person's retirement date has been reached on the given today (on or after it). */
    public boolean retiredOn(LocalDate today) {
        return retiredFrom != null && today != null && !today.isBefore(retiredFrom);
    }

    /** Whether the person is retired: a retirement date is set, whatever the date. */
    public boolean retired() {
        return retiredFrom != null;
    }

    /**
     * Whether a claim on this date is refused: the person is retired from an earlier date, or the date is after the
     * from-date and before the to-date of a retirement they were unretired from.
     */
    public boolean retiredBefore(LocalDate date) {
        return (retiredFrom != null && date.isAfter(retiredFrom))
                || pastRetirements.stream().anyMatch((r) -> r.refuses(date));
    }

    /** The retirement refusing a claim on this date, as "from" for a current one or "from to to" for a past one. */
    public String retirementBefore(LocalDate date) {
        if (retiredFrom != null && date.isAfter(retiredFrom)) {
            return String.valueOf(retiredFrom);
        }
        return pastRetirements.stream().filter((r) -> r.refuses(date)).findFirst()
                .map((r) -> r.from() + " to " + r.to()).orElse(null);
    }
}
