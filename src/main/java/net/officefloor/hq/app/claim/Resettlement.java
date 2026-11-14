package net.officefloor.hq.app.claim;

/** A re-settled claim's fixed due before and after (null: n/a), for {@link RecordClaimResettled}. */
public record Resettlement(String old, String now) {
}
