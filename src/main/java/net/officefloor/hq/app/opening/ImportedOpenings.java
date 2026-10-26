package net.officefloor.hq.app.opening;

import java.util.List;

/** The opening balance paste's lines, every one checked against the sheet, in the order they were typed. */
public record ImportedOpenings(List<Opening> openings) {
}
