package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AddClaim {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val ClaimInput input, ServerHttpConnection connection, ClaimRepository repository,
            SheetRepository sheets, PeriodRepository periods, PersonRepository people, Audit audit, Rollup rollup, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        if (!repository.personExists(id, pid)) {
            throw new BadRequestException("no such person");
        }
        LocalDate date = input.date() != null ? input.date() : sheets.today(id);
        PeriodGuard.open(periods, id, date);
        people.claimAllowed(id, pid, date);
        input.checkCorrection(repository, id, pid);
        Rollup.Snapshot before = rollup.take(id);
        Claim added = repository.add(id, pid, input.amount(), date, input.note(), input.corrects());
        repository.log(id, added.id(), by);
        audit.record("CLAIM_ADDED sheet=" + id + " by=" + by + " person=" + repository.personName(id, pid)
                + " amount=" + added.amount() + " date=" + added.date());
        rollup.record(id, by, before);
        claim.set(added);
    }
}
