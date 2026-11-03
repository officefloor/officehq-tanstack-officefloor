package net.officefloor.hq.app.rate;

import java.util.List;

/** A sheet's rates, by grade then from-date (an undated rate first); a grade with no rate is not listed. And
 *  {@code inherited}: the rates of the sheets its grades fall back to, nearest first (its parent, the parent's parent,
 *  up to three levels up, then the sheet it was cloned from and that one's fallbacks), each listed the same way. And {@code earlier}, {@code inheritedEarlier}: the older
 *  versions of those rates (superseded), which the claims a newer forward-only version does not apply to still use. */
public record Rates(List<Rate> rates, List<List<Rate>> inherited, List<Rate> earlier, List<List<Rate>> inheritedEarlier) {
}
