package net.officefloor.hq.app.rate;

import java.util.List;

/** How many rates an import set, and the sheet's rates after it. */
public record RateImportResponse(int set, List<Rate> rates) {
}
