package net.officefloor.hq.app.claim;

/** Why a claim change was refused, so the screen can tell one 409 from another. */
public record ClaimRefusal(String reason, String message) {
}
