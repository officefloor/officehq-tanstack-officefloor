package net.officefloor.hq.app.person;

/** The person form as submitted: a name and a grade, their own terms in days (blank: none), and for an edit the version of the person it loaded and the
 *  date the grade applies from (blank: the sheet's today). */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms) {

    /** The terms as a number of days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }
}
