package net.officefloor.hq.app.claim;

/** What a share stored: the person it was shared with and their share. */
public record Shared(String with, ClaimShare share) {
}
