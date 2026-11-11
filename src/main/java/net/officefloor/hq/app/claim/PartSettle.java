package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Rollup;

/** A part-settle's settling of the claim split off: the sheet's figures once split, before it was settled. */
public record PartSettle(Rollup split) {
}
