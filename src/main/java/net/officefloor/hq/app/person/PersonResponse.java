package net.officefloor.hq.app.person;

import java.util.List;

/** statementCount is how many statements were taken; statedTotal is the last one's due-total ("n/a" when it
 *  was n/a, "" when none was taken); statementDate is the sheet's today when it was taken ("" when none). */
public record PersonResponse(String id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate) {

    public static PersonResponse from(Person person) {
        boolean stated = person.getStatementCount() > 0;
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                !stated ? "" : person.getStatedTotal() == null ? "n/a" : person.getStatedTotal().setScale(2).toPlainString(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString());
    }

    public static List<PersonResponse> from(List<Person> people) {
        return people.stream().map(PersonResponse::from).toList();
    }
}
