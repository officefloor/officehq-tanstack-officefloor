package net.officefloor.hq.app.claim;

import java.util.List;

/** A sheet's claims, newest first. */
public record ClaimsResponse(List<ClaimResponse> claims) {

    public static ClaimsResponse from(List<Claim> claims) {
        return new ClaimsResponse(claims.stream().map(ClaimResponse::from).toList());
    }
}
