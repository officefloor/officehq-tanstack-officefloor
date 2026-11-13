package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/**
 * A person's fields as the form submits them: a name (required), a grade (A, B or C) and the date the grade
 * is in force from (yyyy-mm-dd; blank is the sheet's today, and is kept blank here) and the person's own terms
 * (a whole number of days, 0 or more; blank: none, kept blank here). An edit also sends the version of the person
 * its editor loaded; an add sends none.
 */
public record PersonRequest(@Size(max = 200) String name, @Size(max = 10) String grade,
        @Size(max = 30) String gradeFrom, Integer version, @Size(max = 30) String terms) {

    /** The terms as a number of days, or null when blank; call only once validated. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }
}
