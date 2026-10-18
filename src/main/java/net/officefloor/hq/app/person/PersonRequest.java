package net.officefloor.hq.app.person;

/**
 * The body of adding or editing a person: a name (required), a grade (A, B or C) and the date the
 * grade applies from (yyyy-mm-dd; empty: the sheet's today; a person's first grade applies from the
 * beginning of time). An edit also names the version of the person it was made against.
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom) {

    /** The grade's from-date, or null for the sheet's today; only call once validated. */
    public java.time.LocalDate gradeFromDate() {
        return gradeFrom == null || gradeFrom.isBlank() ? null : java.time.LocalDate.parse(gradeFrom.trim());
    }
}
