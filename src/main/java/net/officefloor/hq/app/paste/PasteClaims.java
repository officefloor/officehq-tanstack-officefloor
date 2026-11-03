package net.officefloor.hq.app.paste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import org.springframework.web.bind.annotation.PathVariable;

/** Add a draft claim per pasted line, in order, for the person on the sheet with that name (ignoring case).
 *  One change: every line is checked first by the claim form's rules, so a refused line adds nothing and
 *  writes no audit record; otherwise each claim is added and written as CLAIM_ADDED. */
public class PasteClaims {
    private record Checked(Person person, PasteLine line, LocalDate date) {
    }

    public void paste(@PathVariable("sheetId") String sheetId, @Val PasteInput input,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims, SheetRepository sheets,
            PeriodRepository periods, Audit audit, Rollup rollup, Out<PasteResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!people.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        LocalDate entry = sheets.entryDate(id);
        List<Checked> checked = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        int n = 0;
        for (PasteLine line : input.lines()) {
            n++;
            try {
                Person person = people.findByName(id, line.name());
                if (person == null) {
                    throw new BadRequestException("no one on the sheet is named " + line.name());
                }
                LocalDate date = line.claim().date() != null ? line.claim().date() : entry;
                PeriodGuard.open(periods, id, claims.dueBy(id, person.id(), date));
                people.claimAllowed(id, person.id(), date);
                line.claim().checkCorrection(claims, id, person.id());
                line.claim().checkRef(claims, id, null);
                if (line.claim().ref() != null && !refs.add(line.claim().ref().toLowerCase(Locale.ROOT))) {
                    throw new BadRequestException("reference " + line.claim().ref() + " is given twice in the paste");
                }
                checked.add(new Checked(person, line, date));
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        for (Checked c : checked) {
            Rollup.Snapshot before = rollup.take(id);
            Claim added = claims.add(id, c.person().id(), c.line().claim().amount(), c.date(), c.line().claim().note(), null, c.line().claim().ref());
            claims.log(id, added.id(), by);
            audit.record("CLAIM_ADDED sheet=" + id + " by=" + by + " person=" + c.person().name()
                    + " amount=" + added.amount() + " date=" + added.date() + Claim.auditRef(added.ref()));
            if (added.lateEntry()) {
                audit.record("LATE_ENTRY sheet=" + id + " by=" + by + " person=" + c.person().name()
                        + " amount=" + added.amount() + " date=" + added.date());
            }
            rollup.record(id, by, before);
        }
        result.set(new PasteResult(checked.size()));
    }
}
