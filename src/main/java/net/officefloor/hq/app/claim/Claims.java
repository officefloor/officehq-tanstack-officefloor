package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;

/** A sheet's claims, newest first, and their status logs by claim id (see {@link ClaimRepository#logs}). */
public record Claims(List<Claim> claims, Map<String, List<String>> logs) {
}
