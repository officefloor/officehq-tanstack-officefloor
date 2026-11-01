package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/reassign: the id of the person the claim moves to. */
public record ReassignRequest(Long to) {
}
