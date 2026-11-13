package net.officefloor.hq.app.person;

/**
 * The person as they arrived on the other sheet: the person they were merged into on arrival, as the store holds them
 * now, with that merge; or themselves, with no merge (null).
 */
public record Transferred(Person person, Merge merge) {
}
