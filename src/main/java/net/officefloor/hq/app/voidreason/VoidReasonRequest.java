package net.officefloor.hq.app.voidreason;

/** Body of a void reason added to the sheet: its text and who adds it. */
public record VoidReasonRequest(String reason, String user) {
}
