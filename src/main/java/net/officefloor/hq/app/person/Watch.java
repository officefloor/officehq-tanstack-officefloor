package net.officefloor.hq.app.person;

/** The signed-in user's watch of a person after a request: whether they watch them now, and whether
 *  the request changed it. */
public record Watch(long personId, boolean watched, boolean changed) {
}
