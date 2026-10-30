package net.officefloor.hq.app.period;

import java.util.List;

/** How many monthly periods were made, and the sheet's periods after. */
public record GenerateResponse(int generated, List<PeriodResponse> periods) {
}
