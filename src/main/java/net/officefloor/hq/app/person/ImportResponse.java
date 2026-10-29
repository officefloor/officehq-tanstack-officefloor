package net.officefloor.hq.app.person;

/** How many people an import added, and how many it updated. */
public record ImportResponse(int added, int updated) {
}
