package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds each person's open figure at the end of the old year's last period to their opening balance for the new year's
 * first period: the due now of their claims that count then, whose due-by falls in a period of the old year, and that
 * were OPEN or PENDING at that period's end by their status log (a person with none gets no line). Then audits the
 * rollover with how many periods the generator made.
 */
public class CarryOpenings {
    public void carry(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val RolloverYear year, @Val Integer made, Periods periods, Claims claims, People people, Rates rates,
            Settings settings, SheetClock clock, Audit audit) {
        List<Period> old = periods.list(sheet).stream()
                .filter((p) -> !LocalDate.parse(p.from()).isBefore(year.from())
                        && !LocalDate.parse(p.from()).isAfter(year.to()))
                .toList();
        LocalDate end = old.stream().map((p) -> LocalDate.parse(p.to())).max(Comparator.naturalOrder()).orElseThrow();
        Period first = periods.on(sheet, year.next());
        if (first != null) {
            List<Rate> sheetRates = rates.list(sheet);
            Dated floor = settings.floor(sheet);
            Late late = settings.late(sheet, clock.today(sheet));
            Map<Long, BigDecimal> open = new LinkedHashMap<>();
            List<Claim> sheetClaims = claims.list(sheet);
            for (Person person : people.list(sheet)) {
                for (Claim claim : sheetClaims) {
                    if (claim.person() != person.id()) {
                        continue;
                    }
                    LocalDate dueBy = LocalDate.parse(claim.dueBy());
                    boolean inOld = old.stream().anyMatch((p) -> !dueBy.isBefore(LocalDate.parse(p.from()))
                            && !dueBy.isAfter(LocalDate.parse(p.to())));
                    ClaimStatus then = claim.statusOn(end);
                    if (!inOld || (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING)
                            || !person.countsOn(claim, end)) {
                        continue;
                    }
                    BigDecimal due = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
                    if (due != null) {
                        open.merge(person.id(), due, BigDecimal::add);
                    }
                }
                BigDecimal amount = open.get(person.id());
                if (amount != null && amount.signum() != 0) {
                    periods.addOpening(sheet, first.name(), person.id(), amount);
                    audit.record("OPENING_SET sheet=" + sheet + " by=" + user + " name=" + person.name() + " period="
                            + first.name() + " amount=" + amount.toPlainString());
                }
            }
        }
        audit.record("ROLLOVER sheet=" + sheet + " by=" + user + " fy=" + year.fy() + " periods=" + made);
    }
}
