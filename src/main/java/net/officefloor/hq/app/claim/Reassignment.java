package net.officefloor.hq.app.claim;

/** A claim's move to another person: the names of the person it left and the one it went to. */
public record Reassignment(String from, String to) {
}
