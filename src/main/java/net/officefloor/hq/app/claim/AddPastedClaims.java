package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.batch.SettleBatches;
import net.officefloor.hq.app.flag.Flags;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.hq.app.watch.NotifyWatchers;
import net.officefloor.hq.app.watch.Watches;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPastedClaims {
    // All or nothing, in one transaction this step owns: each checked line, in order, becomes a claim added as the claim
    // form adds one (CLAIM_ADDED), or edits the DRAFT it matched to the line's amount (CLAIM_EDITED when it changes), and
    // is then opened by the same steps (and the same lines) as its open button, its ROLLUP and TOTALS lines following.
    // If any is refused, the transaction is rolled back, its lines dropped, and the refusal is the paste's.
    public void add(@PathVariable("sheetId") String sheetId, @Val PastedLines pasted, Claims claims, ClaimLocks locks,
            People people, Rates rates, Sheets sheets, Settings settings, Holidays holidays, Periods periods, Rollup rollup,
            VoidReasons voidReasons, Watches watches, SettleBatches batches, Flags flags, PlatformTransactionManager transactions,
            ServerHttpConnection connection, Audit audit, Out<PasteResponse> result)
            throws ClaimStatusRefusedException, ClaimNoteNeededException, ClaimProofNeededException,
            ClaimVoidReasonNeededException, ClaimInClosedPeriodException, ClaimLockedException, ClaimNotDraftException {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        List<Object[]> lines = new ArrayList<>();
        // Holds the lines until the transaction commits; it writes nothing itself.
        Audit buffer = new Audit(".", null) {
            @Override
            public void record(String entry, LocalDate on) {
                lines.add(new Object[] { entry, on });
            }
        };
        Exception[] refused = new Exception[1];
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            try {
                for (PastedLine line : pasted.lines()) {
                    Figures before = rollup.figures(sheet);
                    Claim claim = line.draft() == null
                            ? add(line, claims, sheets, settings, holidays, rollup, user, buffer)
                            : edit(line, claims, locks, periods, people, user, connection, buffer);
                    ClaimResponse[] changed = new ClaimResponse[1];
                    ClaimStatusRequest[] request = new ClaimStatusRequest[1];
                    new ApplyClaimStatus().apply(ClaimStatus.OPEN.name(), null, claim, claims, people, rates, settings,
                            sheets, periods, rollup, voidReasons, batches, flags, connection, r -> changed[0] = r, r -> request[0] = r);
                    new AuditClaimStatus().audit(claim, changed[0], request[0], people, connection, buffer);
                    new NotifyWatchers().notifyWatchers(claim, changed[0], people, watches, connection, buffer);
                    RollupLines.record(sheet, user, before, rollup, buffer);
                }
            } catch (Exception e) {
                refused[0] = e;
                tx.setRollbackOnly();
            }
        });
        if (refused[0] != null) {
            rethrow(refused[0]);
        }
        for (Object[] line : lines) {
            audit.record((String) line[0], (LocalDate) line[1]);
        }
        result.set(new PasteResponse(pasted.lines().size()));
    }

    /** Adds the line's DRAFT claim as the claim form adds one, recorded as CLAIM_ADDED. */
    private static Claim add(PastedLine line, Claims claims, Sheets sheets, Settings settings, Holidays holidays,
            Rollup rollup, String user, Audit audit) {
        Person person = line.person();
        ClaimInput input = line.input();
        LocalDate today = sheets.today(person.sheetId());
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(person.sheetId());
        int terms = person.termsOn(date, settings);
        LocalDate dueBy = holidays.dueBy(person.sheetId(), date, terms);
        Claim claim = claims.add(person.sheetId(), person.id(), input.amount(), date, input.note(), null, dueBy, terms, input.ref(),
                input.tags());
        claims.log(claim, user.trim(), today, rollup.figure(claim));
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record(claim.ref() == null
                ? Audit.line("CLAIM_ADDED", claim.sheetId(), user, "person=" + person.name(),
                        "amount=" + shown.amount(), "date=" + shown.date())
                : Audit.line("CLAIM_ADDED", claim.sheetId(), user, "person=" + person.name(),
                        "amount=" + shown.amount(), "date=" + shown.date(), "ref=" + claim.ref()));
        if (claim.lateEntry()) {
            audit.record(Audit.line("LATE_ENTRY", claim.sheetId(), user, "person=" + person.name(),
                    "amount=" + shown.amount(), "date=" + shown.date()));
        }
        return claim;
    }

    /**
     * Edits the DRAFT the line matched to the line's amount, as its editor would (CLAIM_EDITED, only when the amount
     * changes); refused as the editor refuses it: no longer a DRAFT, its editor open to another user, its period closed, or
     * its amount its lines'.
     */
    private static Claim edit(PastedLine line, Claims claims, ClaimLocks locks, Periods periods, People people, String user,
            ServerHttpConnection connection, Audit audit) throws ClaimNotDraftException, ClaimLockedException, ClaimInClosedPeriodException,
            ClaimStatusRefusedException {
        Claim claim = claims.find(line.draft().sheetId(), line.draft().id());
        if (claim == null || claim.status() != ClaimStatus.DRAFT) {
            throw new ClaimNotDraftException("The claim a pasted line updates is no longer a DRAFT.");
        }
        String by = locks.lockedBy(claim);
        if (by != null && !by.equals(user.trim())) {
            throw new ClaimLockedException("The claim is being edited by " + by + ".");
        }
        Period closed = periods.closedOn(claim.sheetId(), claim.dueBy());
        if (closed != null) {
            throw new ClaimInClosedPeriodException("The period " + closed.name() + " is closed: its claims cannot change.");
        }
        if (claim.amount().compareTo(line.input().amount()) == 0) {
            return claim;
        }
        if (!claims.lines(claim).isEmpty()) {
            throw new ClaimStatusRefusedException("The claim's amount is its lines added up: it cannot be pasted.");
        }
        Claim edited = claim.with(new ClaimInput(line.input().amount(), claim.date(), claim.note(), claim.corrects(),
                claim.ref(), claim.tags()), claim.date());
        claims.save(edited, user.trim());
        new AuditClaimEdited().audit(claim, ClaimResponse.from(edited), people, connection, audit);
        return edited;
    }

    private static void rethrow(Exception e) throws ClaimStatusRefusedException, ClaimNoteNeededException,
            ClaimProofNeededException, ClaimVoidReasonNeededException, ClaimInClosedPeriodException, ClaimLockedException,
            ClaimNotDraftException {
        if (e instanceof ClaimStatusRefusedException x) throw x;
        if (e instanceof ClaimNoteNeededException x) throw x;
        if (e instanceof ClaimProofNeededException x) throw x;
        if (e instanceof ClaimVoidReasonNeededException x) throw x;
        if (e instanceof ClaimInClosedPeriodException x) throw x;
        if (e instanceof ClaimLockedException x) throw x;
        if (e instanceof ClaimNotDraftException x) throw x;
        if (e instanceof RuntimeException x) throw x;
        throw new IllegalStateException(e);
    }
}
