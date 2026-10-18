package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Closes the period, refusing one already closed, and fixes its due-total: the due of the claims that
 *  count dated in it (not the OPEN claims of a person on hold), as shown now (a settled claim's fixed due; an open one's at the rate in force on its date for the grade in force on its
 *  date, rounded half up when dated before the sheet's policy date (its today when none is set), half to even otherwise, and raised to the sheet's floor unless it is a correction), none where no rate applies. */
public class ClosePeriod {
    public void close(@Val Period period, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets)
            throws InvalidPeriodException {
        if (period.isClosed()) {
            throw new InvalidPeriodException("The period " + period.getName() + " is already closed.");
        }
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        BigDecimal floor = settings.floor(period.getSheetId());
        LocalDate today = sheets.findById(period.getSheetId()).map((s) -> s.getToday()).orElse(null);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.findBySheetIdOrderByIdDesc(period.getSheetId())) {
            if (!c.getStatus().counts() || !period.contains(c.getDate())
                    || (c.getStatus() == ClaimStatus.OPEN && byId.get(c.getPersonId()).isHeld())) {
                continue;
            }
            BigDecimal due = c.getStatus() == ClaimStatus.SETTLED ? c.getSettledDue()
                    : rates.findInForce(c.getSheetId(), byId.get(c.getPersonId()).gradeOn(c.getDate()), c.getDate())
                            .map((r) -> c.getAmount().multiply(r.getRate())
                                    .divide(BigDecimal.valueOf(100), 2, settings.rounding(c.getSheetId(), c.getDate(), today)))
                            .map((d) -> c.isCorrection() ? d : d.max(floor))
                            .orElse(null);
            if (due != null) {
                total = total.add(due);
            }
        }
        period.close(total);
    }
}
