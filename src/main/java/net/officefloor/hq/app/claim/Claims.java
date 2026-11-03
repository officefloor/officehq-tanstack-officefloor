package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;

/** A sheet's claims, newest first, and their status logs by claim id (see {@link ClaimRepository#logs}),
 *  and who holds each locked claim's editor, by claim id (see {@link ClaimRepository#locks}), and every tag ever used on the sheet, sorted,
 *  and their comments by claim id (see {@link ClaimRepository#comments}), and the settle batch of each claim a bulk settle
 *  settled, by claim id, and the number of the sheet's last batch (null when none), and their lines by claim id (see
 *  {@link ClaimRepository#lines}), and their added numbers by claim id (see {@link ClaimRepository#added(String)}), and whether each REVIEW claim is review-late, by claim id (see
 *  {@link ClaimRepository#reviewLate}). */
public record Claims(List<Claim> claims, Map<String, List<String>> logs, Map<String, String> locks, List<String> tags,
        Map<String, List<String>> comments, Map<String, Integer> batches, Integer lastBatch,
        Map<String, List<String>> lines, Map<String, Long> added, Map<String, Boolean> reviewLate) {
}
