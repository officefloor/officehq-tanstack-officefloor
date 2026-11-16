package net.officefloor.hq.app.patron;

/** An add of a patron once validated: the id of the person named as patron, and who does it. */
public record PatronAdd(long patronId, String by) {
}
