package net.officefloor.hq.app.quick;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a draft claim, dated the sheet's today with an empty note, for the person with this name (ignoring
 *  case), adding them first with grade A when the sheet has no one by that name. One change: every check
 *  runs before anything is written, so a refusal leaves no new person and no audit record behind. */
public class QuickAdd {
    public void add(@PathVariable("sheetId") String sheetId, @Val QuickAddInput input,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims, SheetRepository sheets,
            PeriodRepository periods, Audit audit, Rollup rollup, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!people.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        LocalDate date = sheets.today(id);
        PeriodGuard.open(periods, id, date);
        Person person = people.findByName(id, input.name());
        if (person != null) {
            people.claimAllowed(id, person.id(), date);
        }
        if (person == null) {
            Rollup.Snapshot before = rollup.take(id);
            try {
                person = people.add(id, input.name(), "A", null);
            } catch (DuplicateKeyException e) {
                throw new BadRequestException("name is already in use");
            }
            audit.record("PERSON_ADDED sheet=" + id + " by=" + by + " name=" + person.name()
                    + " grade=" + person.grade());
            rollup.record(id, by, before);
        }
        Rollup.Snapshot before = rollup.take(id);
        Claim added = claims.add(id, person.id(), input.amount(), date, "", null);
        claims.log(id, added.id(), by);
        audit.record("CLAIM_ADDED sheet=" + id + " by=" + by + " person=" + person.name()
                + " amount=" + added.amount() + " date=" + added.date());
        rollup.record(id, by, before);
        claim.set(added);
    }
}
