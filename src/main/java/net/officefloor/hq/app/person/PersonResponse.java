package net.officefloor.hq.app.person;

import java.util.List;

/** statementCount is how many statements were taken; statedTotal is the last one's due-total ("n/a" when it
 *  was n/a, "" when none was taken); statementDate is the sheet's today when it was taken ("" when none); retired says whether a retirement date is set and retiredFrom is it ("" when none); held says whether they are on hold; patrons are the names of the people they named as patrons, sorted, and patronIds their ids. */
public record PersonResponse(String id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom, boolean held,
        List<String> patrons, List<String> patronIds) {

    public static PersonResponse from(Person person) {
        boolean stated = person.getStatementCount() > 0;
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                !stated ? "" : person.getStatedTotal() == null ? "n/a" : person.getStatedTotal().setScale(2).toPlainString(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString(),
                person.isRetired(), person.getRetiredFrom() == null ? "" : person.getRetiredFrom().toString(),
                person.isHeld(), List.copyOf(person.getPatronNames()),
                person.getPatronIds().stream().map(String::valueOf).toList());
    }

    public static List<PersonResponse> from(List<Person> people) {
        return people.stream().map(PersonResponse::from).toList();
    }
}
