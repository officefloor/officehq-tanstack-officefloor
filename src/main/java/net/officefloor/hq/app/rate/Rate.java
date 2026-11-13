package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * A rate a sheet has for a grade: a percent with two decimals, in force from {@code from} (null: from
 * the beginning of time) until the grade's next rate takes over, or through {@code to} (null: no end), whichever
 * comes first.
 */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {

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
