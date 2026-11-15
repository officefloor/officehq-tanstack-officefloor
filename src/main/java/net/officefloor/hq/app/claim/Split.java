package net.officefloor.hq.app.claim;

/** A split: the original claim, keeping the rest of its amount, and the new claim split off it. */
public record Split(Claim original, Claim created) {
}
