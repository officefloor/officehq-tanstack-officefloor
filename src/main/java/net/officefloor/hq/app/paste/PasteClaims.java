package net.officefloor.hq.app.paste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAmountEdit;
import net.officefloor.hq.app.claim.ClaimStatusChange;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Add and open a claim per pasted line, in order, for the person on the sheet with that name (ignoring case).
 *  A line naming a date and note that match one of the person's claims as they stood before the paste (not VOID or
 *  DELETED; never a claim the paste itself added) updates the newest such DRAFT to the line's amount instead
 *  (CLAIM_EDITED when it changes) and opens it; a line whose matches hold no DRAFT is refused. Every line is checked
 *  first by the claim form's rules; each claim is then opened exactly as its open button would, in paste order
 *  (unnumbered until settled). One change: any refusal (a line's, or an open's) refuses the whole paste — the store is rolled back and
 *  no audit record is written. */
public class PasteClaims {
    private record Checked(Person person, PasteLine line, LocalDate date, Claim draft) {
    }

    /** Carries a refusal out of the transaction, which it rolls back. */
    private static final class Refused extends RuntimeException {
        final BadRequestException refusal;

        Refused(BadRequestException refusal) {
            super(refusal.getMessage(), null, false, false);
            this.refusal = refusal;
        }
    }

    public void paste(@PathVariable("sheetId") String sheetId, @Val PasteInput input,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims, SheetRepository sheets,
            PeriodRepository periods, SettingsRepository settings, WatchRepository watches, Audit audit, Rollup rollup,
            PlatformTransactionManager transactions, Out<PasteResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!people.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        try {
            result.set(audit.held(() -> transaction.execute(status -> {
                try {
                    return apply(id, by, input, people, claims, sheets, periods, settings, watches, audit, rollup);
                } catch (BadRequestException e) {
                    throw new Refused(e);
                }
            })));
        } catch (Refused e) {
            throw e.refusal;
        }
    }

    private static PasteResult apply(String id, String by, PasteInput input, PersonRepository people, ClaimRepository claims,
            SheetRepository sheets, PeriodRepository periods, SettingsRepository settings, WatchRepository watches,
            Audit audit, Rollup rollup) throws BadRequestException {
        LocalDate entry = sheets.entryDate(id);
        List<Checked> checked = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        // Each person's claims as they stood before the paste, newest first.
        Map<Long, List<Claim>> before = new HashMap<>();
        Set<Long> taken = new HashSet<>();
        int n = 0;
        for (PasteLine line : input.lines()) {
            n++;
            try {
                Person person = people.findByName(id, line.name());
                if (person == null) {
                    throw new BadRequestException("no one on the sheet is named " + line.name());
                }
                LocalDate date = line.claim().date() != null ? line.claim().date() : entry;
                List<Claim> theirs = before.computeIfAbsent(person.id(), pid -> claims.ofPerson(id, pid));
                String note = line.claim().note() == null ? "" : line.claim().note().trim();
                boolean matched = false;
                Claim draft = null;
                for (Claim c : theirs) {
                    if (c.status().equals("VOID") || c.status().equals("DELETED") || !c.date().equals(date.toString())
                            || !(c.note() == null ? "" : c.note().trim()).equals(note)) {
                        continue;
                    }
                    matched = true;
                    if (c.status().equals("DRAFT") && !taken.contains(c.id())) {
                        draft = c;
                        break;
                    }
                }
                if (matched && draft == null) {
                    throw new BadRequestException(person.name() + " has no draft claim of " + date + " with that note to update");
                }
                if (draft != null) {
                    taken.add(draft.id());
                }
                PeriodGuard.open(periods, id, draft != null ? LocalDate.parse(draft.dueBy()) : claims.dueBy(id, person.id(), date));
                people.claimAllowed(id, person.id(), date);
                line.claim().checkCorrection(claims, id, person.id(), draft == null ? null : draft.id());
                line.claim().checkRef(claims, id, draft == null ? null : draft.id());
                if (line.claim().ref() != null && !refs.add(line.claim().ref().toLowerCase(Locale.ROOT))) {
                    throw new BadRequestException("reference " + line.claim().ref() + " is given twice in the paste");
                }
                checked.add(new Checked(person, line, date, draft));
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        n = 0;
        for (Checked c : checked) {
            n++;
            try {
                long cid;
                if (c.draft() != null) {
                    cid = c.draft().id();
                    ClaimAmountEdit.apply(id, cid, c.line().claim().amount(), by, claims, periods, audit, rollup);
                } else {
                    Rollup.Snapshot was = rollup.take(id);
                    Claim added = claims.add(id, c.person().id(), c.line().claim().amount(), c.date(), c.line().claim().note(), null, c.line().claim().ref());
                    claims.log(id, added.id(), by);
                    audit.record("CLAIM_ADDED sheet=" + id + " by=" + by + " person=" + c.person().name()
                            + " amount=" + added.amount() + " date=" + added.date() + Claim.auditRef(added.ref()));
                    if (added.lateEntry()) {
                        audit.record("LATE_ENTRY sheet=" + id + " by=" + by + " person=" + c.person().name()
                                + " amount=" + added.amount() + " date=" + added.date());
                    }
                    rollup.record(id, by, was);
                    cid = added.id();
                }
                ClaimStatusChange.open(id, cid, by, claims, periods, settings, audit, rollup, watches);
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        return new PasteResult(checked.size());
    }
}
