package net.officefloor.hq.app.importing;

/** How many people an import added, and how many it updated. */
public record ImportResult(int added, int updated) {
}
