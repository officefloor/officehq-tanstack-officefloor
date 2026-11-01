package net.officefloor.hq.app.patron;

/** That {@code patron} is one of {@code person}'s patrons: both are ids of people on the same sheet. {@code named}
 *  orders a person's patrons as they were named (lower first). */
public record Patron(long person, long patron, long named) {
}
