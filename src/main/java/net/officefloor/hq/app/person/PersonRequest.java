package net.officefloor.hq.app.person;

/** Body of an add or edit of a person. An edit carries the version of the person its form loaded,
 *  and the date its grade applies from (blank: the sheet's today; an add's grade applies from the
 *  beginning of time), and their own terms in whole days (null: none), and their contact (trimmed and lower-cased; null: none),
 *  and their tags (comma separated; see {@link net.officefloor.hq.app.claim.ClaimForm#tags}; null: none). */
public record PersonRequest(String name, String grade, String gradeFrom, String terms, Long version,
        String contact, String tags) {
}
