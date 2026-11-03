package net.officefloor.hq.app.rate;

import java.util.List;

/** A sheet's rates, by grade then from-date (an undated rate first); a grade with no rate is not listed. And
 *  {@code inherited}: the rates of the sheets its grades fall back to, nearest first (its parent, the parent's parent,
 *  up to three levels up), each listed the same way. */
public record Rates(List<Rate> rates, List<List<Rate>> inherited) {
}
