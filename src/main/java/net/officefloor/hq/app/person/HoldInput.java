package net.officefloor.hq.app.person;

/** A validated hold: the from-date already defaulted to the sheet's today. */
public record HoldInput(Hold hold, String user) {
}
