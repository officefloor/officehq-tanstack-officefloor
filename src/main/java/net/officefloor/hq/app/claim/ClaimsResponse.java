package net.officefloor.hq.app.claim;

import java.util.List;

/** The claims on a sheet, newest first. */
public record ClaimsResponse(List<ClaimResponse> claims) {
}
