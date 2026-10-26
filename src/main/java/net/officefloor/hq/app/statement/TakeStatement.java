package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class TakeStatement {
    // Records the person's due-total now and binds each of their counting claims (a held person's open claims do not count) to its figure now;
    // the rest of their claims are left unbound. The late charge is never bound: the due-total includes it,
    // but each claim is bound to the figure under it.
    public void take(@Val Person person, Claims claims, Rates rates, Settings settings, Sheets sheets, Statements statements,
            Out<Statement> statementOut) {
        statements.lock(person.id());
        LocalDate today = sheets.today(person.sheetId());
        Map<Long, BigDecimal> figures = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.list(person.sheetId())) {
            if (claim.personId() == person.id()
                    && (claim.status() == ClaimStatus.SETTLED || (claim.status().isOpen() && !person.held()))) {
                BigDecimal figure = figure(claim, person, rates, settings);
                figures.put(claim.id(), figure);
                BigDecimal due = claim.settledDue() != null ? figure : settings.withLate(claim, figure, today);
                if (due != null) {
                    total = total.add(due);
                }
            }
        }
        // With no rate today and no figure, the due-total is n/a.
        boolean none = rates.on(person.sheetId(), person.gradeOn(today), today) == null
                && figures.values().stream().allMatch(Objects::isNull);
        claims.bind(person.sheetId(), person.id(), figures);
        Statement statement = new Statement(person.sheetId(), person.id(), none ? "n/a" : total.toPlainString(),
                figures.size(), today);
        statements.add(statement);
        statementOut.set(statement);
    }

    /**
     * The claim's figure now (null when it has none): a settled claim's frozen due, otherwise the figure the
     * last statement bound it to, otherwise its amount times its person's grade's rate on its date, rounded by the policy date, raised to the floor.
     */
    private static BigDecimal figure(Claim claim, Person person, Rates rates, Settings settings) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.gradeOn(claim.date()), claim.date());
        return settings.live(claim, rate);
    }
}
