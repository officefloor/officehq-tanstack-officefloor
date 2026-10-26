package net.officefloor.hq.app.claim;

/** A split: the claim as it is left ({@code original}) and the new claim split off it ({@code created}). */
public record Split(Claim original, Claim created) {
}
