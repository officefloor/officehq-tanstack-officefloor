package net.officefloor.hq.app.claim;

import java.util.List;

/** How many of the selected claims were opened and how many refused, with the sheet's claims after. */
public record BulkOpenResponse(int opened, int refused, List<Claim> claims) {
}
