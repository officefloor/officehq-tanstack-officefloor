package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Take a person's statement: record their due-total now, and bind each claim of theirs that counts (a
 *  person's open (OPEN or PENDING) claims dated within a hold do not, unless already bound) to the figure it has now (a settled claim keeps its frozen due); a
 *  claim that does not count is unbound (live again). A late charge is never bound: it keeps following
 *  the sheet's today. The due-total is as shown: the sum of the counting claims' dues now (late charges
 *  included), skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today
 *  is in force then and no counting claim has a fixed figure or a due (at the grade it was claimed under). */
public class TakeStatement {
    public void take(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims,
            SheetRepository sheets, Audit audit, Rollup rollup, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = people.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        LocalDate today = sheets.today(id);
        Rollup.Snapshot before = rollup.take(id);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int count = 0;
        for (Claim c : claims.ofPerson(id, pid)) {
            if (!c.counts(found.holds())) {
                claims.bind(id, c.id(), false, null);
                continue;
            }
            count++;
            fixed |= c.fixed();
            // The claim is bound to its figure; its late charge is not bound, but counts in the due-total.
            BigDecimal figure = claims.figure(id, c);
            BigDecimal now = claims.dueNow(id, c);
            if (now != null) {
                total = total.add(now);
                fixed = true;
            }
            claims.bind(id, c.id(), true, figure);
        }
        String dueTotal = fixed || claims.rated(id, pid, today) ? total.setScale(2).toPlainString() : "n/a";
        people.stated(id, pid, dueTotal, today);
        audit.record("STATEMENT sheet=" + id + " by=" + by + " name=" + found.name() + " due-total=" + dueTotal
                + " claims=" + count);
        rollup.record(id, by, before);
        person.set(people.find(id, pid));
    }
}
