package net.officefloor.hq.app.person;

/** A validated flag: the reason trimmed and the until-date already defaulted to the sheet's today. */
public record FlagInput(Flag flag, String user) {
}
