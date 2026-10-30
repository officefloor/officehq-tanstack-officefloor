package net.officefloor.hq.app.crew;

/** A crew that passed validation: its name trimmed, not blank and not yet a crew on the sheet; its lead a person of the sheet. */
public record ValidCrew(String name, long leadId, String leadName, String user) {
}
