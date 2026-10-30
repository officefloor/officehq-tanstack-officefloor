package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people and PUT /api/sheets/{sheetId}/people/{personId}.
 *  {@code version} (PUT only) is the version of the person the editor loaded; {@code gradeFrom}
 *  (PUT only, YYYY-MM-DD) is the date the grade is recorded from, blank for the sheet's today;
 *  {@code terms} is the person's own terms in whole days, blank for none; {@code contact} is the
 *  person's contact, trimmed and lower-cased, blank for none; {@code tags} are the person's tags, comma separated (sorted and comma separated once checked), blank for none;
 *  {@code termsFrom} (PUT only, YYYY-MM-DD) is the date the terms are recorded from, blank for the beginning of time. */
public record PersonRequest(String name, String grade, String user, Integer version, String gradeFrom, String terms, String contact, String tags, String termsFrom) {
}
