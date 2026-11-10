package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person, as the screen reads it (their grade the one in force on the sheet's today), with their statement count, and the last statement's due-total
 * and date ("" before the first), whether they are retired and the date they are retired from
 * ("" when not), whether they have any hold, their holds (in the order added), the ids of the people they name as patrons (in the order named) with their
 * shares (see {@link #shares}), and every
 * grade they have been given with the date it is from (the first "", from the beginning of time),
 * by from-date, their own terms in days ("" for none), their contact ("" for none), and their aliases
 * (in the order given).
 */
public record PersonResponse(String id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<HoldFrom> holds, java.util.List<String> patronIds, List<String> patronShares, List<GradeFrom> grades, String terms,
        String contact, List<String> aliases) {

    /** A hold from a date to a date (yyyy-MM-dd; to "" when open-ended), both inclusive. */
    public record HoldFrom(String from, String to) {}

    /** A grade in force from a date (yyyy-MM-dd; "" from the beginning of time). */
    public record GradeFrom(String from, String grade) {}

    /**
     * The patrons' shares, in the order named: 100.00 split equally among {@code count} patrons, each
     * rounded half to even to two decimals, the remainder (100.00 less the shares added up) to the first.
     */
    static List<String> shares(int count) {
        if (count == 0) return List.of();
        BigDecimal whole = new BigDecimal("100.00");
        BigDecimal each = whole.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        List<String> shares = new ArrayList<>();
        shares.add(whole.subtract(each.multiply(BigDecimal.valueOf(count - 1))).toPlainString());
        for (int i = 1; i < count; i++) shares.add(each.toPlainString());
        return shares;
    }

    /** The person as the screen reads it on the sheet's today: their grade is the one in force on it. */
    public static PersonResponse from(Person person, LocalDate today) {
        List<GradeFrom> grades = new ArrayList<>();
        grades.add(new GradeFrom("", person.getGrade()));
        person.getDatedGrades().forEach((from, grade) -> grades.add(new GradeFrom(from.toString(), grade)));
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.gradeOn(today),
                person.getVersion(), person.getStatementCount(),
                person.getStatedTotal() == null ? "" : person.getStatedTotal(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString(),
                person.isRetired(), person.getRetiredFrom() == null ? "" : person.getRetiredFrom().toString(),
                person.isHeld(),
                person.getHolds().stream().map((h) -> new HoldFrom(h.from().toString(), h.to() == null ? "" : h.to().toString())).toList(),
                person.getPatronIds().stream().map(String::valueOf).toList(), shares(person.getPatronIds().size()), grades,
                person.getTerms() == null ? "" : person.getTerms().toString(),
                person.getContact() == null ? "" : person.getContact(), List.copyOf(person.getAliases()));
    }
}
