package net.officefloor.hq.app.claim;

/** A split as stored: the original claim keeping the rest of its amount, and the new claim split off it. */
public record ClaimSplit(Claim original, Claim created) {
}
