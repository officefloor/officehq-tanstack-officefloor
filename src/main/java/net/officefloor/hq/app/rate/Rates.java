package net.officefloor.hq.app.rate;

import java.util.List;

/** A sheet's rates, by grade; a grade with no rate is not listed. */
public record Rates(List<Rate> rates) {
}
