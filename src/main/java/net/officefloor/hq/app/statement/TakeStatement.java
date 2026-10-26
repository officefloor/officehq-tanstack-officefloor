package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class TakeStatement {
    // Records the person's due-total now and binds each of their counting claims to its figure now;
    // the rest of their claims are left unbound.
    public void take(@Val Person person, Claims claims, Rates rates, Sheets sheets, Statements statements,
            Out<Statement> statementOut) {
        statements.lock(person.id());
        LocalDate today = sheets.today(person.sheetId());
        Map<Long, BigDecimal> figures = new LinkedHashMap<>();
        for (Claim claim : claims.list(person.sheetId())) {
            if (claim.personId() == person.id()
                    && (claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.SETTLED)) {
                figures.put(claim.id(), figure(claim, person, rates));
            }
        }
        BigDecimal total = figures.values().stream().filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        // With no rate today and no figure, the due-total is n/a.
        boolean none = rates.on(person.sheetId(), person.grade(), today) == null
                && figures.values().stream().allMatch(Objects::isNull);
        claims.bind(person.sheetId(), person.id(), figures);
        Statement statement = new Statement(person.sheetId(), person.id(), none ? "n/a" : total.toPlainString(),
                figures.size(), today);
        statements.add(statement);
        statementOut.set(statement);
    }

    /**
     * The claim's figure now (null when it has none): a settled claim's frozen due, otherwise the figure the
     * last statement bound it to, otherwise its amount times its person's grade's rate on its date, half to even.
     */
    private static BigDecimal figure(Claim claim, Person person, Rates rates) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated()) {
            return claim.statedDue();
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null : claim.amount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
