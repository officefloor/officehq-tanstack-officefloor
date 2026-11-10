package net.officefloor.hq.app.claim;

import java.util.List;

/** The claims on a sheet, newest first, and every tag ever used on it, sorted. */
public record ClaimsResponse(List<ClaimResponse> claims, List<String> tags) {
}
