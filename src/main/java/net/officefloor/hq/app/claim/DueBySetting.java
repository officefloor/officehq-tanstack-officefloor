package net.officefloor.hq.app.claim;

/** A claim's due-by set directly: the due-by it had and the one it has now. */
public record DueBySetting(String oldDueBy, String newDueBy) {
}
