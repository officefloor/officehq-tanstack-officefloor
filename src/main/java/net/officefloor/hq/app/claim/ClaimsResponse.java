package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A sheet's claims, newest first, each with its status log. */
public record ClaimsResponse(List<ClaimResponse> claims) {

    public static ClaimsResponse from(List<Claim> claims, List<ClaimLogEntry> log) {
        Map<Long, List<ClaimLogEntry>> byClaim = log.stream().collect(Collectors.groupingBy(ClaimLogEntry::getClaimId));
        return new ClaimsResponse(claims.stream()
                .map((c) -> ClaimResponse.from(c, byClaim.getOrDefault(c.getId(), List.of()))).toList());
    }
}
