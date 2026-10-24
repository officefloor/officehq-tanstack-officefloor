package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.statement.Statement;

/** A person as the screen shows them: their grade in force on the sheet's today, and every grade they were given
 *  with its from-date (null for the first, which applies from the beginning of time), with their statement count
 *  and their last statement's due-total and date (null with none), and the date they are retired from (null: not
 *  retired), and whether they have any hold, and their holds (earliest first), and the ids of their patrons in the order they were named, and their own terms in days (null: none), and their contact (null: none), and their aliases, sorted ignoring case, and whether
 *  they clash: their name or an alias is in use on another sheet of the sheet's tree, and the sheet they were
 *  transferred from (null: none). */
public record PersonResponse(long id, String name, String grade, List<DatedGrade> grades, long version,
        int statementCount, String statedTotal, String statementDate, String retiredFrom, boolean held,
        List<Hold> holds, List<Long> patronIds, Integer terms,
        String contact, List<String> aliases, boolean clashing, String origin) {

    /** A hold from a date on, to a date (inclusive; null: open-ended). */
    public record Hold(String from, String to) {
    }

    /** A grade from a date on (null: from the beginning of time). */
    public record DatedGrade(String grade, String from) {
    }

    /** @param statements the person's statements, oldest first.
     *  @param patrons the person's patrons, in the order they were named.
     *  @param grades the person's grades over time.
     *  @param aliases the person's aliases.
     *  @param today the sheet's today.
     *  @param usedAround the names and aliases, lower-cased, in use on the other sheets of the sheet's tree. */
    public static PersonResponse from(Person person, List<Statement> statements, List<Patron> patrons,
            List<PersonAlias> aliases, PersonGrades grades, LocalDate today,
            java.util.Set<String> usedAround) {
        Statement last = statements.isEmpty() ? null : statements.get(statements.size() - 1);
        List<DatedGrade> dated = new ArrayList<>();
        dated.add(new DatedGrade(grades.first(), null));
        grades.dated().forEach(g -> dated.add(new DatedGrade(g.getGrade(), g.getFromDate().toString())));
        return new PersonResponse(person.getId(), person.getName(), grades.on(today), dated, person.getVersion(),
                statements.size(), last == null ? null : last.getDueTotal(),
                last == null ? null : last.getTakenOn().toString(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(), person.isHeld(),
                person.getHolds().stream().map(h -> new Hold(h.getFromDate().toString(),
                        h.getToDate() == null ? null : h.getToDate().toString())).toList(),
                patrons.stream().map(Patron::getPatronId).toList(), person.getTerms(), person.getContact(),
                aliases.stream().map(PersonAlias::getAlias)
                        .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder()))
                        .toList(),
                usedAround.contains(TreeNames.key(person.getName()))
                        || aliases.stream().anyMatch(a -> usedAround.contains(TreeNames.key(a.getAlias()))),
                person.getOriginSheet());
    }
}
