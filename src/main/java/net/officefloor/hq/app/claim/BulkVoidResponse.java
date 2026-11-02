package net.officefloor.hq.app.claim;

import java.util.List;

/** How many of the selected claims were voided and how many refused, with the sheet's claims after. */
public record BulkVoidResponse(int voided, int refused, List<Claim> claims) {
}
