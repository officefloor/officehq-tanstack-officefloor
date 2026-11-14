package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class AddClaim {
    public void add(@RequestParam(name = "by", required = false) String by,
            @Val ValidClaim request, @Val Person person, SheetRepository sheets,
            ClaimRepository claims, PeriodRepository periods, Out<Claim> claim)
            throws PeriodClosedException, InvalidClaimException {
        String user = by == null ? "" : by;
        LocalDate date = request.date() != null ? request.date() : sheets.today(person.getSheetId());
        if (person.retiredBefore(date)) {
            throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
        }
        periods.requireOpen(person.getSheetId(), date.toString());
        claim.set(claims.add(person.getSheetId(), person.getId(), request.amount(), date,
                request.note(), user));
    }
}
