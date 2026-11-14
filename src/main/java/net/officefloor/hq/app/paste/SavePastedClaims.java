package net.officefloor.hq.app.paste;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ApplyClaimAction;
import net.officefloor.hq.app.claim.AuditClaimAction;
import net.officefloor.hq.app.claim.AuditClaimEdited;
import net.officefloor.hq.app.claim.CheckClaimDraft;
import net.officefloor.hq.app.claim.CheckClaimLinesAmount;
import net.officefloor.hq.app.claim.CheckClaimNotLocked;
import net.officefloor.hq.app.claim.CheckClaimPeriodOpen;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAction;
import net.officefloor.hq.app.claim.ClaimActionRequest;
import net.officefloor.hq.app.claim.ClaimDueNow;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLockRepository;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimNumber;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.LateEntries;
import net.officefloor.hq.app.claim.LogClaimStatus;
import net.officefloor.hq.app.claim.NewDueBy;
import net.officefloor.hq.app.claim.PreviousDue;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes each pasted line in order, and opens its claim as the open button would. A line matching a DRAFT updates it as
 * the claim's editor would, to the line's amount (recorded as CLAIM_EDITED when the amount changes); any other line
 * adds a DRAFT claim as the claim form's add does: due-by fixed from its date and its person's grade's terms, its
 * DRAFT status logged, and recorded as CLAIM_ADDED (with its reference, if any) (and LATE_ENTRY when its period is
 * soft-closed). Then the claim is opened by the user: numbered, logged, recorded as CLAIM_OPENED and followed by the
 * rollup lines it moved. Any line the editor or the open button would refuse refuses the whole paste: the transaction
 * is rolled back and no record is written.
 */
public class SavePastedClaims {
    public void save(@Val PastedLines pasted, @RequestParam("user") String user, SheetRepository sheets,
            SettingRepository settings, HolidayRepository holidays, PersonRepository people, RateRepository rates,
            PeriodRepository periods, VoidReasonRepository voidReasons, ClaimRepository claims, ClaimLogRepository logs,
            ClaimLockRepository locks, ClaimLineRepository claimLines, RollupService rollups, Audit audit,
            LateEntries lateEntries, Out<PasteResult> result) throws InvalidClaimException {
        int[] done = { 0 };
        audit.holding(() -> {
            int number = 0;
            for (PastedLine line : pasted.lines()) {
                number++;
                Person person = line.person();
                Rollup before = rollups.compute(person.sheetId());
                try {
                    Claim draft = line.draft() == null
                            ? add(line, person, user, sheets, settings, holidays, people, rates, claims, logs, audit,
                                    lateEntries)
                            : edit(line, person, user, claims, locks, claimLines, periods, people, audit);
                    open(draft, user, sheets, people, rates, settings, periods, voidReasons, claims, logs, audit);
                } catch (InvalidClaimException e) {
                    throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
                }
                RollupLines.record(before, rollups.compute(person.sheetId()), person.sheetId(), user, audit);
                done[0]++;
            }
        });
        result.set(new PasteResult(done[0]));
    }

    private static Claim add(PastedLine line, Person person, String user, SheetRepository sheets,
            SettingRepository settings, HolidayRepository holidays, PersonRepository people, RateRepository rates,
            ClaimRepository claims, ClaimLogRepository logs, Audit audit, LateEntries lateEntries) {
        LocalDate date = line.input().date() != null ? line.input().date() : sheets.entryDate(person.sheetId());
        LocalDate dueBy = NewDueBy.of(date, person, sheets, settings, holidays);
        Claim claim = claims.save(new Claim(null, person.sheetId(), person.id(), line.input().amount(), date,
                line.input().note(), ClaimStatus.DRAFT, null, null, null, false, null, line.input().corrects(),
                dueBy));
        BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
        logs.append(claim.sheetId(), new ClaimLogLine(claim.id(), claim.status(), user,
                sheets.today(claim.sheetId()), due == null ? "n/a" : due.setScale(2).toPlainString()));
        claims.saveRef(claim, line.input().ref());
        audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + user + " person=" + person.name()
                + " amount=" + claim.amount().setScale(2).toPlainString() + " date=" + claim.date()
                + Claim.auditRef(line.input().ref()));
        lateEntries.mark(claim, person, user);
        return claim;
    }

    /** The matched DRAFT, edited to the line's amount as the claim's editor would (when the amount changes). */
    private static Claim edit(PastedLine line, Person person, String user, ClaimRepository claims,
            ClaimLockRepository locks, ClaimLineRepository claimLines, PeriodRepository periods,
            PersonRepository people, Audit audit) throws InvalidClaimException {
        Claim claim = claims.find(person.sheetId(), line.draft());
        if (claim == null) {
            throw new InvalidClaimException("The claim it matched is no longer on the sheet.");
        }
        new CheckClaimNotLocked().check(claim, user, locks);
        new CheckClaimDraft().check(claim);
        new CheckClaimPeriodOpen().check(claim, periods);
        new CheckClaimLinesAmount().check(claim, line.input(), claimLines);
        if (line.input().ref() != null) {
            claims.saveRef(claim, line.input().ref());
        }
        if (claim.amount().compareTo(line.input().amount()) == 0) {
            return claim;
        }
        Claim edited = claims.save(new Claim(claim.id(), claim.sheetId(), claim.personId(), line.input().amount(),
                claim.date(), claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(),
                claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(),
                claim.proofs(), claim.splitFrom(), claim.added(), claim.waived(), claim.held()));
        claims.markEdited(edited.sheetId(), edited.id(), user);
        new AuditClaimEdited().audit(edited, people, user, audit);
        return edited;
    }

    /** Opens the DRAFT exactly as the open button does: refused on the same terms. */
    private static void open(Claim draft, String user, SheetRepository sheets, PersonRepository people,
            RateRepository rates, SettingRepository settings, PeriodRepository periods,
            VoidReasonRepository voidReasons, ClaimRepository claims, ClaimLogRepository logs, Audit audit)
            throws InvalidClaimException {
        new CheckClaimPeriodOpen().check(draft, periods);
        ClaimActionRequest request = new ClaimActionRequest("", "");
        ClaimAction[] action = new ClaimAction[1];
        ClaimNumber[] number = new ClaimNumber[1];
        PreviousDue[] previous = new PreviousDue[1];
        Claim[] updated = new Claim[1];
        new ApplyClaimAction().apply("open", user, draft, request, sheets, people, rates, settings, periods,
                voidReasons, claims, logs, (v) -> action[0] = v, (v) -> number[0] = v, (v) -> previous[0] = v,
                (v) -> updated[0] = v);
        Claim opened = claims.save(updated[0]);
        new LogClaimStatus().log(opened, user, sheets, people, rates, settings, claims, logs);
        new AuditClaimAction().audit(action[0], number[0], opened, request, previous[0], people, user, audit);
    }
}
