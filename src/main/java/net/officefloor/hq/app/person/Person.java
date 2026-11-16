package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person on a sheet: a name (unique on the sheet, case ignored; kept without surrounding spaces) and a grade, one of A, B or C.
 *  The grade is dated: {@code grades} are the grades recorded, by from-date (the one with none, the first given, first), and
 *  {@code grade} is the one in force on the sheet's today.
 *  The version is 0 when added and goes up by one on each stored edit. {@code retiredFrom} (yyyy-MM-dd) is the date the
 *  person is retired from, null when not retired; a person with one counts as retired at once, whatever the date.
 *  {@code holds} are the person's holds, by from-date: an OPEN or PENDING claim of theirs dated within one, and not bound by
 *  a statement, counts in no live total. {@code held} is whether they have any hold. {@code terms} are the person's own
 *  terms (days a claim of theirs has until it is due by, ahead of their grade's and the sheet's) in force on the sheet's
 *  today, null when none; {@code ownTerms} are their own terms as recorded, by from-date (the one from the beginning of
 *  time, with none, first), a claim having those in force on its date. {@code contact} is the person's contact (unique on the sheet; kept without
 *  surrounding spaces, lower-cased), null when none. {@code aliases} are the person's aliases, sorted as names are compared
 *  (each kept without surrounding spaces; no alias equals, case ignored, any name or alias on the sheet). {@code clashing}
 *  is whether their name or an alias is used as a name or alias on another sheet of the sheet's tree (see
 *  PersonRepository#treeKeys): set only on the people listed, false elsewhere. {@code origin} is the sheet the person was
 *  last transferred from, null when they were never transferred here. {@code retirements} are the person's past
 *  retirements (each ended by unretiring them), in the order they ended. {@code tags} are the person's tags (each trimmed and
 *  in lower case, no repeats, sorted; empty when none): a claim of theirs counts for each in the tag totals. */
public record Person(long id, String sheetId, String name, String grade, long version, String retiredFrom, boolean held,
        List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact, List<String> aliases,
        boolean clashing, String origin, List<PersonRetirement> retirements, List<String> tags, List<PersonTerms> ownTerms) {

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom, boolean held,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact, List<String> aliases,
            boolean clashing, String origin, List<PersonRetirement> retirements, List<String> tags) {
        this(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin,
                retirements, tags, List.of(new PersonTerms(terms, null)));
    }

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom, boolean held,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact, List<String> aliases,
            boolean clashing, String origin, List<PersonRetirement> retirements) {
        this(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin,
                retirements, List.of());
    }

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom, boolean held,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact, List<String> aliases) {
        this(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, false, null,
                List.of());
    }

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact, List<String> aliases) {
        this(id, sheetId, name, grade, version, retiredFrom, !holds.isEmpty(), grades, holds, terms, contact, aliases);
    }

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact) {
        this(id, sheetId, name, grade, version, retiredFrom, grades, holds, terms, contact, List.of());
    }

    /** The person with whether they clash in the sheet's tree. */
    public Person withClashing(boolean clashing) {
        return new Person(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin, retirements, tags, ownTerms);
    }

    /** The person with the sheet they were transferred from (null: none). */
    public Person withOrigin(String origin) {
        return new Person(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin, retirements, tags, ownTerms);
    }

    /** The person with their past retirements. */
    public Person withRetirements(List<PersonRetirement> retirements) {
        return new Person(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin, retirements, tags, ownTerms);
    }

    /** The person with their tags. */
    public Person withTags(List<String> tags) {
        return new Person(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin, retirements, tags, ownTerms);
    }

    /** The person with their own terms as recorded, and {@code terms} those in force on the sheet's today. */
    public Person withOwnTerms(List<PersonTerms> ownTerms, Integer terms) {
        return new Person(id, sheetId, name, grade, version, retiredFrom, held, grades, holds, terms, contact, aliases, clashing, origin, retirements, tags, ownTerms);
    }

    /** The person's own terms from the beginning of time (null: none). */
    public Integer baseTerms() {
        return ownTerms.stream().filter((t) -> t.from() == null).findFirst().map(PersonTerms::days).orElse(null);
    }

    /** The person's own terms in force on a date (null: none, so their grade's or the sheet's). */
    public Integer termsOn(LocalDate date) {
        return termsOn(ownTerms, date);
    }

    /** The own terms in force on a date among these: the ones with the latest from-date on or before it (a null from-date
     *  is the beginning of time); null when there are none. */
    public static Integer termsOn(List<PersonTerms> terms, LocalDate date) {
        PersonTerms found = null;
        for (PersonTerms t : terms) {
            if (t.from() != null && t.from().isAfter(date)) {
                continue;
            }
            if (found == null || found.from() == null || (t.from() != null && t.from().isAfter(found.from()))) {
                found = t;
            }
        }
        return found == null ? null : found.days();
    }

    /** The person not retired, the retirement ended on the date kept among their past ones. */
    public Person unretired(LocalDate on) {
        List<PersonRetirement> past = new java.util.ArrayList<>(retirements);
        past.add(new PersonRetirement(LocalDate.parse(retiredFrom), on));
        return new Person(id, sheetId, name, grade, version, null, held, grades, holds, terms, contact, aliases, clashing, origin, List.copyOf(past), tags, ownTerms);
    }

    /** Whether the person's name or any alias is among the name keys (lower-cased names and aliases). */
    public boolean usesAny(java.util.Set<String> keys) {
        return keys.contains(PersonRepository.key(name)) || aliases.stream().anyMatch((a) -> keys.contains(PersonRepository.key(a)));
    }

    /** Whether a hold of the person covers the date. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch((h) -> h.covers(date));
    }

    /** Whether the person's retirement date is reached on the day: it is on or before it. From then on their OPEN or
     *  PENDING claims not bound by a statement count in no live total, as a hold's would. */
    public boolean retiredOn(LocalDate date) {
        return retiredFrom != null && !LocalDate.parse(retiredFrom).isAfter(date);
    }

    public boolean retired() {
        return retiredFrom != null;
    }

    /** Whether a claim of the person may be dated {@code date}: not after their retirement date, nor after the from and
     *  before the to of a past retirement. */
    public boolean mayClaimOn(java.time.LocalDate date) {
        return (retiredFrom == null || !date.isAfter(java.time.LocalDate.parse(retiredFrom)))
                && retirements.stream().noneMatch((r) -> r.excludes(date));
    }

    /** The grade in force on a date: the one with the latest from-date on or before it. */
    public String gradeOn(LocalDate date) {
        return gradeOn(grades, date);
    }

    /** The grade in force on a date among these, by from-date; null when there are none. */
    public static String gradeOn(List<PersonGrade> grades, LocalDate date) {
        PersonGrade found = null;
        for (PersonGrade g : grades) {
            if (g.from() != null && g.from().isAfter(date)) {
                continue;
            }
            if (found == null || found.from() == null || (g.from() != null && g.from().isAfter(found.from()))) {
                found = g;
            }
        }
        return found == null ? null : found.grade();
    }
}
