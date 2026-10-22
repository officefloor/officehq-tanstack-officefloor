package net.officefloor.hq.app.sheet;

/** A clone that went through: the sheet cloned and the sheet it was cloned to. */
public record CloneResponse(String fromSheet, String toSheet) {
}
