package net.officefloor.hq.app.person;

/** How many people an import added, and how many people already on the sheet it updated. */
public record ImportResponse(int added, int updated) {
}
