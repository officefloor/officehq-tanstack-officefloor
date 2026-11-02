package net.officefloor.hq.app.period;

import java.util.List;

/** How many pasted opening-balance lines went through, and every period on the sheet after them. */
public record OpeningImportResponse(int set, List<Period> periods) {
}
