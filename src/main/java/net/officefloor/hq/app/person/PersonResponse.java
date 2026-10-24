package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.statement.Statement;

/** A person as the screen shows them: their grade in force on the sheet's today, and every grade they were given
 *  with its from-date (null for the first, which applies from the beginning of time), with their statement count
 *  and their last statement's due-total and date (null with none), and the date they are retired from (null: not
 *  retired), and whether they have any hold, and their holds (earliest first), and the ids of their patrons in the order they were named, and their own terms in days (null: none). */
public record PersonResponse(long id, String name, String grade, List<DatedGrade> grades, long version,
        int statementCount, String statedTotal, String statementDate, String retiredFrom, boolean held,
        List<Hold> holds, List<Long> patronIds, Integer terms) {

    /** A hold from a date on, to a date (inclusive; null: open-ended). */
    public record Hold(String from, String to) {
    }

    /** A grade from a date on (null: from the beginning of time). */
    public record DatedGrade(String grade, String from) {
    }

    /** @param statements the person's statements, oldest first.
     *  @param patrons the person's patrons, in the order they were named.
     *  @param grades the person's grades over time.
     *  @param today the sheet's today. */
    public static PersonResponse from(Person person, List<Statement> statements, List<Patron> patrons,
            PersonGrades grades, LocalDate today) {
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
                patrons.stream().map(Patron::getPatronId).toList(), person.getTerms());
    }
}
