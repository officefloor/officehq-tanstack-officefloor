package net.officefloor.hq.app.person;

import java.util.List;

/** A person, with the grade in force on the sheet's today and every grade they were given (each from its from-date), their statement count and their last statement's due-total ("n/a" when it had none) and date
 *  (both null before their first statement), the date they are retired from (null: not retired), whether they have any hold and their holds (in the order added), their patrons' names, sorted, their own terms (null: none) and their contact (null: none). */
public record PersonResponse(long id, String name, String grade, List<GradeResponse> grades, long version, int statementCount, String statedTotal,
        String statementDate, String retiredFrom, boolean held, List<HoldResponse> holds,
        List<String> patrons, Integer terms, String contact) {
    public static PersonResponse from(Person person) {
        Statements s = person.getStatements();
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(),
                person.getGrades().stream().map(GradeResponse::from).toList(), person.getVersion(), s.count(),
                s.count() == 0 ? null : s.lastDueTotal() == null ? "n/a" : s.lastDueTotal().toPlainString(),
                s.lastDate() == null ? null : s.lastDate().toString(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(), person.isHeld(),
                person.getHolds().stream().map(HoldResponse::from).toList(),
                person.getPatrons(), person.getTerms(), person.getContact());
    }
}
