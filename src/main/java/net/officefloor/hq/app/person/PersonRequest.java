package net.officefloor.hq.app.person;

/**
 * Body of POST /api/sheets/{sheet}/people and PUT /api/sheets/{sheet}/people/{person}. On a PUT, the
 * version is the one the editor loaded (null to save regardless), and the grade is recorded from the
 * from-date (yyyy-mm-dd; blank for the sheet's today). On a POST the grade applies from the beginning of time. The terms are the person's own, a whole number of days
 * (blank: none), recorded from the terms from-date (yyyy-mm-dd; blank: from the beginning of time). The contact is trimmed and lower-cased (blank: none). The tags are comma separated (absent: kept as
 * they are on a PUT).
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact, String tags,
        String termsFrom) {

    /** A request with no terms from-date: its terms apply from the beginning of time. */
    public PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact,
            String tags) {
        this(name, grade, version, gradeFrom, terms, contact, tags, null);
    }

    /** The terms the form gave, in days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }

    /** The contact the form gave; null when blank. */
    public String contactOrNull() {
        return contact == null || contact.isEmpty() ? null : contact;
    }

    /** The tags the form gave: each trimmed and lower-cased, blanks and repeats dropped, sorted; null when absent. */
    public java.util.List<String> tagList() {
        return tags == null ? null : net.officefloor.hq.app.claim.ClaimInput.tagsOf(tags);
    }
}
