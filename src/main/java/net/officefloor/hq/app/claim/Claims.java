package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;

/** A sheet's claims, newest first, and their status logs by claim id (see {@link ClaimRepository#logs}),
 *  and who holds each locked claim's editor, by claim id (see {@link ClaimRepository#locks}), and every tag ever used on the sheet, sorted. */
public record Claims(List<Claim> claims, Map<String, List<String>> logs, Map<String, String> locks, List<String> tags) {
}
