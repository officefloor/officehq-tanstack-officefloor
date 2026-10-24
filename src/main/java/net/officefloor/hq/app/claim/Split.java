package net.officefloor.hq.app.claim;

/** A split's new claim: the OPEN claim made with the amount split off the original. */
public record Split(Claim made) {
}
