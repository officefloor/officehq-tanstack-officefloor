package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Rollup;

/** A part-settle: the split that made the new claim, the sheet's figures between the split and the settle, and the
 *  new claim's number as its settle left it. */
public record PartSettle(Split split, Rollup between, String number) {
}
