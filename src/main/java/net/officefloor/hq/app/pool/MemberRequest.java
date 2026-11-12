package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheetId}/pools/{poolId}/members: the person, their share and the user adding them. */
public record MemberRequest(Long personId, String share, String user) {
}
