package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class TakeStatement {
    // Records the person's due-total now (a deferred claim counts in none and is left unbound) (held to the sheet's cap less their cut total) and binds each of their counting claims (their open claims within a hold and not bound do not count) to its figure now;
    // the rest of their claims are left unbound. Each claim is bound to its due as shown, late charge and floor included, so
    // while bound it no longer follows the sheet's today; a claim already bound keeps the figure it was bound to.
    // Taken as of a date (on or before the sheet's today), it is worked out from the person and their claims as they stood at
    // the end of that date, with that date for the sheet's today; it binds those claims still theirs, and records that date.
    public void take(@Val Person person, @Val StatementAsOf asOf, Claims claims, People people, Rates rates, Settings settings,
            Sheets sheets, PoolCuts poolCuts, Statements statements, Out<Statement> statementOut) throws StatementRefusedException {
        statements.lock(person.id());
        LocalDate today = sheets.today(person.sheetId());
        Stated stated;
        if (asOf.date() == null) {
            stated = stated(person, today, claims, rates, settings, poolCuts);
        } else {
            if (asOf.date().isAfter(today)) {
                throw new StatementRefusedException("A statement cannot be taken as of a date after the sheet's today.");
            }
            stated = AsOf.on(asOf.date(), () -> {
                Person then = people.find(person.sheetId(), person.id());
                return then == null ? null : stated(then, asOf.date(), claims, rates, settings, poolCuts);
            });
            if (stated == null) {
                throw new StatementRefusedException(person.name() + " was not on the sheet on " + asOf.date() + ".");
            }
            // A claim since moved to another person is no longer theirs to bind.
            Set<Long> theirs = claims.list(person.sheetId()).stream().filter(c -> c.personId() == person.id())
                    .map(Claim::id).collect(Collectors.toSet());
            stated.figures().keySet().retainAll(theirs);
        }
        claims.bind(person.sheetId(), person.id(), stated.figures());
        Statement statement = new Statement(person.sheetId(), person.id(), stated.dueTotal(), stated.figures().size(),
                asOf.date() == null ? today : asOf.date(), asOf.date());
        statements.add(statement);
        statementOut.set(statement);
    }

    /** What a statement binds (each counting claim's figure, by id) and records (the due-total). */
    private record Stated(Map<Long, BigDecimal> figures, String dueTotal) {
    }

    /** The person's statement with {@code today} as the sheet's today. */
    private static Stated stated(Person person, LocalDate today, Claims claims, Rates rates, Settings settings,
            PoolCuts poolCuts) {
        Map<Long, BigDecimal> figures = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.list(person.sheetId())) {
            if (claim.personId() == person.id()
                    && (claim.status() == ClaimStatus.SETTLED || (claim.status().isOpen() && Rollup.counts(claim, person, today)))) {
                BigDecimal due = due(claim, person, today, rates, settings);
                figures.put(claim.id(), due);
                if (due != null) {
                    total = total.add(due);
                }
            }
        }
        // With no rate today and no figure, the due-total is n/a.
        boolean none = rates.on(person.sheetId(), person.gradeOn(today), today) == null
                && figures.values().stream().allMatch(Objects::isNull);
        return new Stated(figures, none ? "n/a" : settings.capped(person.sheetId(), total,
                poolCuts.totals(person.sheetId()).getOrDefault(person.id(), BigDecimal.ZERO.setScale(2))).toPlainString());
    }

    /**
     * The claim's due as shown with {@code today} as the sheet's today (null when it has none): a settled claim's frozen due,
     * otherwise the due the last statement bound it to, otherwise its amount times its person's grade's rate on its date,
     * rounded by the policy date, raised to the floor, with its late charge on {@code today} (worked out on the floored figure) added on top.
     */
    private static BigDecimal due(Claim claim, Person person, LocalDate today, Rates rates, Settings settings) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated()) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.forClaim(claim, person.gradeOn(claim.date()));
        return settings.withLate(claim, settings.live(claim, rate), today);
    }
}
