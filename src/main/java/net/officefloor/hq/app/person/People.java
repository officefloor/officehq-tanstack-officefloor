package net.officefloor.hq.app.person;

import java.util.List;

/** A sheet's people, in the order they were added, and every patron any of them has named, and the ids of
 *  the people the signed-in user watches. */
public record People(List<Person> people, List<Patron> patrons, List<Long> watched) {
}
