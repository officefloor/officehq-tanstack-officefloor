package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person as the screen sees it, with their statement count, and the last statement's due-total
 *  (two decimals, null when it was n/a or none was taken) and date (yyyy-mm-dd, null when none was),
 *  and whether they are retired and from when (yyyy-mm-dd, null when not retired), and each past
 *  retirement's dates, in the order they ended,
 *  and whether they have any hold and each hold's dates, and the ids of the people they have named as patrons. `grade` is
 *  the grade in force on the sheet's today; `grades` every grade recorded, by from-date (null: from
 *  the beginning of time, first); `terms` the person's own terms in force on the sheet's today (null: none); `contact` their
 *  contact (null: none); `aliases` their aliases, sorted as names are compared; `clashing` whether
 *  their name or an alias is used on another sheet of their sheet's tree; `origin` the sheet they were
 *  transferred from (null: they were not); `tags` their tags, sorted (empty: none);
 *  `snapshots` their snapshot lines, oldest first ("<day left> <due-total>");
 *  `statementLines` their statements still standing, oldest first ("<statement date> <due-total>");
 *  `ownTerms` their own terms from the beginning of time (null: none); `datedTerms` every dated terms
 *  recorded, by from-date (`terms` null: none from then). */
public record PersonResponse(long id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        List<RetirementDates> pastRetirements,
        boolean held, List<HoldDates> holds, List<Long> patrons, List<GradeFrom> grades, Integer terms,
        String contact, List<String> aliases, boolean clashing, String origin, List<String> tags,
        List<String> snapshots, List<String> statementLines, Integer ownTerms, List<TermsFrom> datedTerms) {

    /** A hold's dates (yyyy-mm-dd; `to` null: open-ended). */
    public record HoldDates(String from, String to) {
    }

    /** A past retirement's dates (yyyy-mm-dd): retired from `from`, unretired on `to`. */
    public record RetirementDates(String from, String to) {
    }

    public record GradeFrom(String grade, String from) {
    }

    public record TermsFrom(Integer terms, String from) {
    }

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() != null ? person.getStatedTotal().toPlainString() : null,
                person.getStatementDate() != null ? person.getStatementDate().toString() : null,
                person.isRetired(), person.getRetiredFrom() != null ? person.getRetiredFrom().toString() : null,
                person.getPastRetirements().stream()
                        .map(r -> new RetirementDates(r.from().toString(), r.to().toString()))
                        .toList(),
                person.isHeld(),
                person.getHolds().stream()
                        .map(h -> new HoldDates(h.from().toString(), h.to() != null ? h.to().toString() : null))
                        .toList(),
                List.copyOf(person.getPatronIds()),
                person.getGrades().entrySet().stream()
                        .map(e -> new GradeFrom(e.getValue(), e.getKey().equals(LocalDate.MIN) ? null : e.getKey().toString()))
                        .toList(),
                person.getTermsInForce(), person.getContact(), List.copyOf(person.getAliases()),
                person.isClashing(), person.getOriginSheet(),
                person.getTags() == null ? List.of() : List.of(person.getTags().split(",")),
                List.copyOf(person.getSnapshots()), List.copyOf(person.getStatementLines()), person.getTerms(),
                person.getDatedTerms().entrySet().stream()
                        .map(e -> new TermsFrom(e.getValue(), e.getKey().toString()))
                        .toList());
    }
}
