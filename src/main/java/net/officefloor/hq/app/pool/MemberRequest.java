package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheetId}/pools/{poolId}/members: the person or the member pool (one of them), the
 *  share and the user adding them. */
public record MemberRequest(Long personId, Long memberPoolId, String share, String user) {
}
