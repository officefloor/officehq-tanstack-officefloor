package net.officefloor.hq.app.claim;

import java.util.List;

/** How many of the selected claims were re-dated and refused, and the sheet's claims after. */
public record BulkRedateResponse(int redated, int refused, List<Claim> claims) {
}
