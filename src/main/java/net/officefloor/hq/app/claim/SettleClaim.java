package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Settles a claim, fixing its due: its amount at its person's grade's rate in force on its date, rounded half to even to
 *  two decimals. With no rate the due is not fixed and stays live. Refused for the user who opened it (four eyes). */
public class SettleClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates) throws ClaimStatusException {
        ClaimStatus settled = ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.SETTLED);
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by == null ? "" : by)) {
            throw new ClaimStatusException("The user who opened a claim cannot settle it");
        }
        claim.setStatus(settled.name());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.getGrade(),
                LocalDate.parse(claim.getDate()));
        claim.setSettledDue(rate == null ? null
                : new BigDecimal(claim.getAmount()).multiply(new BigDecimal(rate.pct()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN).toPlainString());
    }
}
