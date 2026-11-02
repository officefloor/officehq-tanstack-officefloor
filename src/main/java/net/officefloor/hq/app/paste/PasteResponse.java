package net.officefloor.hq.app.paste;

import java.util.List;
import net.officefloor.hq.app.claim.Claim;

/** How many lines the paste took (each claim added or updated, then opened), with the sheet's claims after. */
public record PasteResponse(int added, List<Claim> claims) {
}
