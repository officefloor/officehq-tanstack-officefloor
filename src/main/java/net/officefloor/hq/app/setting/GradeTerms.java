package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.Map;

/**
 * Validated terms for grades, in whole days, by grade; a grade's value null: none of its own (the sheet's terms). A grade
 * left out is left as it is. {@code from} is the from-date typed for each grade (a grade left out: none typed, the sheet's today).
 */
public record GradeTerms(Map<String, Integer> values, Map<String, LocalDate> from) {

    /** The from-date typed for the grade (null: none, the sheet's today). */
    public LocalDate fromOf(String grade) {
        return from.get(grade);
    }
}
