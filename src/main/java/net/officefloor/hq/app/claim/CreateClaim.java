package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class CreateClaim {
    public void create(@Val ValidClaim request, @Val Person person, SheetClock clock, ClaimRepository repository,
            Out<Claim> claim) {
        var date = request.date() != null ? request.date() : clock.today(person.getSheetId());
        claim.set(repository.create(person.getSheetId(), person.getId(), request.amount(), date, request.note(),
                request.corrects()));
    }
}
