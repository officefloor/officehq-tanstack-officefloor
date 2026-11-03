package net.officefloor.hq.app.person;

/** The person form; {@code gradeFrom} is the date the grade is in force from (yyyy-mm-dd; blank for the
 *  sheet's today; an edit only), {@code terms} the person's own terms in days (blank for none), {@code contact} their contact (trimmed and lower-cased; blank for none), {@code tags} their tags (comma separated; kept as {@link net.officefloor.hq.app.claim.Claim#tags(String)} keeps them), {@code version} the version the editor loaded (an edit only). */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact, String tags) {
}
