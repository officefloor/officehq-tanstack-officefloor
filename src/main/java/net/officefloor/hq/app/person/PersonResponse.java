package net.officefloor.hq.app.person;

import java.util.List;

/** A person, with the grade in force on the sheet's today and every grade they were given (each from its from-date), their statement count and their last statement's due-total ("n/a" when it had none) and date
 *  (both null before their first statement), the date they are retired from (null: not retired), their past retirements (in the order kept), whether they have any hold and their holds (in the order added), their patrons' names, sorted, their patrons with their shares of 100.00, sorted by name, their own terms in force on the sheet's today (null: none), their contact (null: none), their tags, sorted (empty: none), their aliases, sorted as names are compared, whether they clash (their name or an alias used on another sheet of their tree) the sheet they were transferred from (null: none) their snapshot lines, oldest first, and their statement lines, one per statement still standing, oldest first, their own terms from the beginning of time (null: none) and the own terms they were given from a date, oldest from-date first, and their flag's reason while it holds on the sheet's today (null: none). */
public record PersonResponse(long id, String name, String grade, List<GradeResponse> grades, long version, int statementCount, String statedTotal,
        String statementDate, String retiredFrom, List<RetirementResponse> retirements, boolean held, List<HoldResponse> holds,
        List<String> patrons, List<PatronShareResponse> patronShares, Integer terms, String contact, List<String> tags, List<String> aliases,
        boolean clashing, String origin, List<String> snapshots,
        List<String> statementLines, Integer ownTerms, List<TermsResponse> datedTerms, String flagShown) {
    public static PersonResponse from(Person person) {
        Statements s = person.getStatements();
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(),
                person.getGrades().stream().map(GradeResponse::from).toList(), person.getVersion(), s.count(),
                s.count() == 0 ? null : s.lastDueTotal() == null ? "n/a" : s.lastDueTotal().toPlainString(),
                s.lastDate() == null ? null : s.lastDate().toString(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(),
                person.getRetirements().stream().map(RetirementResponse::from).toList(), person.isHeld(),
                person.getHolds().stream().map(HoldResponse::from).toList(),
                person.getPatrons(), person.getPatronShares().stream().map(PatronShareResponse::from).toList(),
                person.getTermsToday(), person.getContact(),
                person.getTags() == null ? List.of() : List.of(person.getTags().split(",")), person.getAliases(), person.isClashing(),
                person.getOrigin(), person.getSnapshots().stream().map(SnapshotLine::line).toList(),
                person.getStatementLines().stream().map(StatementLine::line).toList(), person.getTerms(),
                person.getDatedTerms().stream().sorted(java.util.Comparator.comparing(TermsFrom::from))
                        .map(TermsResponse::from).toList(), person.getFlagShown());
    }
}
