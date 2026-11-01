package net.officefloor.hq.app.claim;

import java.util.List;

/** How many of the selected claims were reassigned and refused, and the sheet's claims after. */
public record BulkReassignResponse(int reassigned, int refused, List<Claim> claims) {
}
