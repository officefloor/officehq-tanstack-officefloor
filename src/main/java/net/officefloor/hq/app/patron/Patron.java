package net.officefloor.hq.app.patron;

/** That {@code patron} is one of {@code person}'s patrons: both are ids of people on the same sheet. */
public record Patron(long person, long patron) {
}
