package net.officefloor.hq.app.claim;

/** A claim's move: the names of the person it moved from and the person it moved to. */
public record Reassignment(String from, String to) {
}
