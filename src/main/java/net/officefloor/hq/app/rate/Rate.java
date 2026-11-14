package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;

/**
 * A rate a sheet has for a grade: a percent with two decimals, in force from {@code from} (null: from
 * the beginning of time) until the grade's next rate takes over, or through {@code to} (null: no end), whichever
 * comes first. A rate is kept as versions, one per time it was set for its grade and from-date: this is the newest
 * (the one in force), {@code version} its number (from 1), so how many versions it has. A rate saved with a from-date
 * on or after the sheet's today is {@code forward}-only: it applies only to claims added after it was saved ({@code saved},
 * on the sequence a claim's {@link Claim#added} is on); a claim added before keeps the {@code earlier} version that applied
 * to it (null: none, so the grade has no rate for that from-date for it).
 */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version,
        boolean forward, long saved, Rate earlier) {

    /** The same version with the one before it. */
    public Rate withEarlier(Rate before) {
        return new Rate(sheetId, grade, rate, from, to, version, forward, saved, before);
    }

    /** Whether this version applies to a claim added at {@code added}. */
    public boolean appliesTo(long added) {
        return !forward || added > saved;
    }

    /** The newest version of this rate that applies to a claim added at {@code added}; null with none. */
    public Rate forClaim(long added) {
        Rate version = this;
        while (version != null && !version.appliesTo(added)) {
            version = version.earlier();
        }
        return version;
    }

    /**
     * The rate for the claim's person's grade on the claim's date, as {@link #inForce(List, String, LocalDate)}, from the
     * versions of the rates that apply to the claim.
     */
    public static BigDecimal inForce(List<Rate> rates, String grade, Claim claim) {
        return inForce(rates.stream().map((r) -> r.forClaim(claim.added())).filter(Objects::nonNull).toList(), grade,
                claim.date());
    }

    /**
     * The grade's rate in force on the date: the one with the latest from-date on or before it, unless its to-date
     * is before the date. The rates may be those of a sheet followed by its parent's (and so on up): the first sheet,
     * in that order, with a rate in force gives it; null with none.
     */
    public static BigDecimal inForce(List<Rate> rates, String grade, LocalDate date) {
        Map<String, List<Rate>> bySheet = new LinkedHashMap<>();
        rates.forEach((r) -> bySheet.computeIfAbsent(r.sheetId(), (k) -> new ArrayList<>()).add(r));
        for (List<Rate> sheetRates : bySheet.values()) {
            BigDecimal rate = ownInForce(sheetRates, grade, date);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    private static BigDecimal ownInForce(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream()
                .filter((r) -> r.grade().equals(grade) && (r.from() == null || !r.from().isAfter(date)))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter((r) -> r.to() == null || !r.to().isBefore(date))
                .map(Rate::rate).orElse(null);
    }
}
