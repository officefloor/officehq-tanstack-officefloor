package net.officefloor.hq.app.claim;

import java.util.List;

/**
 * The claims on a sheet, newest first, and every tag ever used on it, sorted, and the number of its
 * last bulk settle batch ("" when there has been none).
 */
public record ClaimsResponse(List<ClaimResponse> claims, List<String> tags, String lastBatch) {
}
