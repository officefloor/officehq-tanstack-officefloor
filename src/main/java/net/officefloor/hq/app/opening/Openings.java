package net.officefloor.hq.app.opening;

import java.util.List;

/** Every line of a paste, checked against the sheet, in the order pasted. */
public record Openings(List<Opening> openings) {
}
