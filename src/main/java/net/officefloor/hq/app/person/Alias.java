package net.officefloor.hq.app.person;

/** One of a person's aliases: another name they are known by on the sheet. */
public record Alias(long personId, String alias) {
}
