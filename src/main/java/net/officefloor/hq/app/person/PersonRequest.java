package net.officefloor.hq.app.person;

/** The person form as submitted: a name and a grade, their own terms in days (blank: none), their contact (trimmed and lower-cased; blank: none), their tags (comma separated; as stored, null for none), and for an edit the version of the person it loaded and the
 *  date the grade applies from (blank: the sheet's today). */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms,
        String contact, String tags) {

    /** The terms as a number of days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }

    /** The contact; null when blank. */
    public String contactOrNull() {
        return contact == null || contact.isEmpty() ? null : contact;
    }
}
