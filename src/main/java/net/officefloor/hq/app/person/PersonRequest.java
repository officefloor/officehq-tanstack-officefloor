package net.officefloor.hq.app.person;

/** A person's fields, the signed-in user (`by`) making the change, and — for an edit — the version
 *  of the person the form loaded (null on an add). */
public record PersonRequest(String name, String grade, String by, Long version) {
}
