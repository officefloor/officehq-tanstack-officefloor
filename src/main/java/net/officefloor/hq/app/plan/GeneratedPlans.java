package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.hq.app.claim.Claim;

/** How many draft claims the plans made, with the sheet's claims and plans after. */
public record GeneratedPlans(int made, List<Claim> claims, List<Plan> plans) {
}
