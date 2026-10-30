package net.officefloor.hq.app.person;

import java.util.List;

/** statementCount is how many statements were taken; statedTotal is the last one's due-total ("n/a" when it
 *  was n/a, "" when none was taken); statementDate is the sheet's today when it was taken ("" when none); retired says whether a retirement date is set and retiredFrom is it ("" when none); held says whether they have any hold and holds lists each (from, to; to "" when open-ended), in the order added; patrons are the names of the people they named as patrons, sorted, and patronIds their ids; grade is the grade in force on the sheet's today and grades every grade recorded, by from-date; terms is the person's own terms in days ("" for none); contact is the person's contact ("" for none); tags are the person's tags, sorted and comma separated ("" for none); aliases are the person's aliases, sorted as names are compared; clashing says whether their name or an alias is used by someone else on a sheet of the sheet's tree; origin is the sheet they were transferred from ("" for none); retirements are their past retirements, each "<from> to <to>", in the order they ended. */
public record PersonResponse(String id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom, boolean held,
        List<HoldFrom> holds,
        List<String> patrons, List<String> patronIds, List<GradeFrom> grades, String terms, String contact, String tags,
        List<String> aliases, boolean clashing, String origin, List<String> retirements) {

    /** A grade in force from a date (YYYY-MM-DD; "" for the beginning of time). */
    public record GradeFrom(String from, String grade) {
    }

    /** A hold from a date to a date (YYYY-MM-DD; to "" when open-ended). */
    public record HoldFrom(String from, String to) {
    }

    public static PersonResponse from(Person person) {
        boolean stated = person.getStatementCount() > 0;
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                !stated ? "" : person.getStatedTotal() == null ? "n/a" : person.getStatedTotal().setScale(2).toPlainString(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString(),
                person.isRetired(), person.getRetiredFrom() == null ? "" : person.getRetiredFrom().toString(),
                person.isHeld(),
                person.getHolds().stream().map(h -> new HoldFrom(h.from().toString(), h.to() == null ? "" : h.to().toString())).toList(),
                List.copyOf(person.getPatronNames()),
                person.getPatronIds().stream().map(String::valueOf).toList(),
                person.getGrades().entrySet().stream().map(e -> new GradeFrom(
                        e.getKey().equals(Person.BEGINNING) ? "" : e.getKey().toString(), e.getValue())).toList(),
                person.getTerms() == null ? "" : String.valueOf(person.getTerms()),
                person.getContact() == null ? "" : person.getContact(),
                person.getTags() == null ? "" : person.getTags(),
                List.copyOf(person.getAliases()), person.isClashing(),
                person.getOrigin() == null ? "" : person.getOrigin(),
                person.getRetirements().stream().map(r -> r.from() + " to " + r.to()).toList());
    }

    public static List<PersonResponse> from(List<Person> people) {
        return people.stream().map(PersonResponse::from).toList();
    }
}
