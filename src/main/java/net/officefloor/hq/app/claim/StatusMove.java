package net.officefloor.hq.app.claim;

/** The move a status change makes: from the claim's stored status to {@code to}, which is PENDING
 *  for a clerk settling a claim above the clerk limit, else the status asked for. */
public record StatusMove(ClaimStatus from, ClaimStatus to) {
}
