package net.officefloor.hq.app.claim;

/** How many of the selected claims were settled (a clerk's settle that left one PENDING counts) and how many
 *  were refused. */
public record BulkSettleResponse(int settled, int refused) {
}
