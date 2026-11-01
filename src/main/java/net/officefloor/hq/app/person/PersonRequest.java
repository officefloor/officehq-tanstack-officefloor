package net.officefloor.hq.app.person;

/**
 * Body of POST /api/sheets/{sheet}/people and PUT /api/sheets/{sheet}/people/{person}. On a PUT, the
 * version is the one the editor loaded (null to save regardless), and the grade is recorded from the
 * from-date (yyyy-mm-dd; blank for the sheet's today). On a POST the grade applies from the beginning of time. The terms are the person's own, a whole number of days
 * (blank: none).
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms) {

    /** The terms the form gave, in days; null when blank. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }
}
