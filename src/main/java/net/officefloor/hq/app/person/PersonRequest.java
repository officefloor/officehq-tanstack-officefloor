package net.officefloor.hq.app.person;

/** A person's fields, the signed-in user (`by`) making the change, — for an edit — the version
 *  of the person the form loaded (null on an add), and the date the grade is given from (blank or null: the sheet's
 *  today; an add's grade applies from the beginning of time), and the person's own terms (blank or null: none; validated,
 *  a whole number of days). */
public record PersonRequest(String name, String grade, String by, Long version, String gradeFrom, String terms) {

    /** The terms as a number of days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }
}
