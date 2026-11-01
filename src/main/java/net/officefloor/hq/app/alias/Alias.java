package net.officefloor.hq.app.alias;

/** One of {@code person}'s aliases: another name the person is found by on the sheet. */
public record Alias(long person, String alias) {
}
