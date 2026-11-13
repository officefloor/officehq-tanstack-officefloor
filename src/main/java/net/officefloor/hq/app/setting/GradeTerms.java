package net.officefloor.hq.app.setting;

import java.util.Map;

/** Validated terms for grades, in whole days, by grade; a grade's value null: none of its own (the sheet's terms). A grade left out is left as it is. */
public record GradeTerms(Map<String, Integer> values) {
}
