package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Take a person's statement: record their due-total now, and bind each claim of theirs that counts
 *  to the figure it has now (a settled claim keeps its frozen due); a claim that does not count is
 *  unbound (live again). The due-total is as shown: the sum of the counting claims' figures, skipping
 *  those with none; 'n/a' when no rate for their grade is in force on the sheet's today and no
 *  counting claim has a fixed figure. */
public class TakeStatement {
    public void take(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims,
            SheetRepository sheets, Audit audit, Out<Person> person) throws BadRequestException {
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
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int count = 0;
        for (Claim c : claims.ofPerson(id, pid)) {
            if (!c.counts()) {
                claims.bind(id, c.id(), false, null);
                continue;
            }
            count++;
            fixed |= c.due() != null || (c.stated() && c.statedDue() != null);
            BigDecimal figure = claims.figure(id, c);
            if (figure != null) {
                total = total.add(figure);
            }
            claims.bind(id, c.id(), true, figure);
        }
        String dueTotal = fixed || claims.rated(id, pid, today) ? total.setScale(2).toPlainString() : "n/a";
        people.stated(id, pid, dueTotal, today);
        audit.record("STATEMENT sheet=" + id + " by=" + by + " name=" + found.name() + " due-total=" + dueTotal
                + " claims=" + count);
        person.set(people.find(id, pid));
    }
}
