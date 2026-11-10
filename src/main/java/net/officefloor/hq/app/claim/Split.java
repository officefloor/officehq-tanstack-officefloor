package net.officefloor.hq.app.claim;

/** A split: the new OPEN claim made with the amount split off the original (the request's claim). */
public record Split(Claim made) {
}
