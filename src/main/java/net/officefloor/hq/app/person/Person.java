package net.officefloor.hq.app.person;

/** A person on a sheet: the id the store assigned, their name and grade (A, B or C), and the
 *  version of these values (up by one on each save). */
public record Person(long id, String name, String grade, long version) {
}
