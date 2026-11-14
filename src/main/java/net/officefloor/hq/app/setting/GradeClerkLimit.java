package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Validated clerk limits for grades, with two decimals, by grade; a grade's value null: blank, none (the sheet's).
 * A grade left out is left as it is. {@code from} is the from-date typed for each grade (a grade left out: none typed,
 * the sheet's today).
 */
public record GradeClerkLimit(Map<String, BigDecimal> values, Map<String, LocalDate> from) {

    /** The from-date typed for the grade (null: none, the sheet's today). */
    public LocalDate fromOf(String grade) {
        return from.get(grade);
    }
}
