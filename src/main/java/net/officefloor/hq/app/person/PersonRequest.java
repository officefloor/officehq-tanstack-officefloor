package net.officefloor.hq.app.person;

/** A person's fields, the signed-in user (`by`) making the change, — for an edit — the version
 *  of the person the form loaded (null on an add), and the date the grade is given from (blank or null: the sheet's
 *  today; an add's grade applies from the beginning of time), and the person's own terms (blank or null: none; validated,
 *  a whole number of days), and their contact (trimmed and lower-cased; blank or null: none), and their tags (comma separated; blank or
 *  null: none; validated, sorted and joined by commas), and the date their terms are given from (blank or null: from the
 *  beginning of time). */
public record PersonRequest(String name, String grade, String by, Long version, String gradeFrom, String terms,
        String contact, String tags, String termsFrom) {

    /** The terms as a number of days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }

    /** The date the terms are given from; null when blank (from the beginning of time). */
    public java.time.LocalDate termsFromDate() {
        return termsFrom == null || termsFrom.isEmpty() ? null : java.time.LocalDate.parse(termsFrom);
    }

    /** The contact; null when blank. */
    public String contactOrNull() {
        return contact == null || contact.isEmpty() ? null : contact;
    }

    /** The tags, sorted and joined by commas; null when blank. */
    public String tagsOrNull() {
        return tags == null || tags.isEmpty() ? null : tags;
    }
}
