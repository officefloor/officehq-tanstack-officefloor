package net.officefloor.hq.app.plan;

import java.util.List;

/** The plans on a sheet, in the order they were added. */
public record PlansResponse(List<PlanResponse> plans) {
}
