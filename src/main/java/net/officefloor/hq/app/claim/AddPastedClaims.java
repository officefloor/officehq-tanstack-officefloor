package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes the checked lines in the order pasted. A line that adds a claim adds it as a DRAFT as AddClaim adds one,
 *  writing CLAIM_ADDED (with ref=<reference> at its end when it has one; and LATE_ENTRY after it for a late entry); a
 *  line that updates a DRAFT edits it to the line's amount as the claim's editor would, writing CLAIM_EDITED when the
 *  amount changes. Either claim is then opened by the user pasting as the open button opens it (see ApplyClaimStatus,
 *  SaveClaimStatus): one with an empty note whose due is above the note threshold, or one that would take its person
 *  past its period's cap (those opened before it in the paste count), refuses the whole paste; so does a paste whose lines would
 *  take a person past the sheet's maximum of open claims (see ClaimRules#overMaxOpen), each line counted as one more, checked
 *  before any line is written. It stays unnumbered (numbered at settlement, see SaveClaimStatus),
 *  writes CLAIM_OPENED (or, its due above the review threshold, goes to REVIEW and writes CLAIM_REVIEW, see
 *  ApplyClaimStatus#needsReview) and the figures the line moved (see Rollup). No line is written to the audit file until every
 *  line has gone through; a refusal writes nothing (the transaction gives back what was stored: the lines are written
 *  in a transaction of their own, as the repositories are not governed by the pipeline's). */
public class AddPastedClaims {
    public void add(@PathVariable("sheetId") String sheetId, @Val PastedClaims checked, @Val PastedLines pasted,
            ClaimRepository claims, PersonRepository people, RateRepository rates, SettingRepository settings,
            PeriodRepository periods, SheetRepository sheets, ClaimLog log, Rollup rollup, Audit audit,
            PlatformTransactionManager transactions, Out<PasteClaimsResult> result) throws InvalidClaimException {
        InvalidClaimException[] refused = new InvalidClaimException[1];
        List<String> lines = new TransactionTemplate(transactions).execute((status) -> {
            try {
                return write(sheetId, checked, pasted, claims, people, rates, settings, periods, sheets, log, rollup);
            } catch (InvalidClaimException e) {
                status.setRollbackOnly();
                refused[0] = e;
                return null;
            }
        });
        if (refused[0] != null) {
            throw refused[0];
        }
        lines.forEach(audit::record);
        result.set(new PasteClaimsResult(checked.claims().size()));
    }

    private static List<String> write(String sheetId, PastedClaims checked, PastedLines pasted, ClaimRepository claims,
            PersonRepository people, RateRepository rates, SettingRepository settings, PeriodRepository periods,
            SheetRepository sheets, ClaimLog log, Rollup rollup) throws InvalidClaimException {
        String sheet = sheetId.trim();
        String by = pasted.by();
        Integer maxOpen = settings.maxOpen(sheet);
        if (maxOpen != null) {
            Map<Long, Integer> opening = new HashMap<>();
            for (PastedClaim c : checked.claims()) {
                long personId = c.person().id();
                int n = opening.merge(personId, 1, Integer::sum);
                if (claims.held(sheet, personId) + n > maxOpen) {
                    throw new InvalidClaimException("The person already has as many open claims as the sheet's maximum allows");
                }
            }
        }
        List<String> lines = new ArrayList<>();
        for (PastedClaim c : checked.claims()) {
            RollupBefore before = rollup.take(sheet);
            String person = c.person().name();
            Claim claim;
            if (c.draft() == null) {
                LocalDate date = c.checked().date();
                claim = claims.add(sheet, c.person().id(), c.values().amount(), date, c.values().note(),
                        c.checked().corrects(), c.values().ref(), c.values().tags(), c.checked().dueBy(), c.checked().terms());
                log.write(claim, by);
                lines.add("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + by + " person=" + person
                        + " amount=" + claim.amount().toPlainString() + " date=" + claim.date()
                        + (claim.ref() == null ? "" : " ref=" + claim.ref()));
                if (periods.softClosedOn(sheet, c.checked().dueBy())) {
                    claims.markLateEntry(claim);
                    lines.add("LATE_ENTRY sheet=" + claim.sheetId() + " by=" + by + " person=" + person
                            + " amount=" + claim.amount().toPlainString() + " date=" + claim.date());
                }
            } else {
                Claim draft = c.draft();
                claim = draft;
                if (c.values().amount().compareTo(draft.amount()) != 0) {
                    claim = new Claim(draft.id(), draft.sheetId(), draft.personId(), c.values().amount(), draft.date(),
                            draft.note(), draft.status(), draft.number(), draft.due(), draft.openedBy(), draft.stated(),
                            draft.statedDue(), draft.corrects(), draft.dueBy(), draft.deferredUntil(), draft.proofs(),
                            draft.splitFrom(), draft.ref(), draft.tags(), draft.held());
                    if (!claims.update(claim, by, sheets.today(sheet))) {
                        throw new InvalidClaimException("Only a DRAFT claim no one else is editing can be edited");
                    }
                    lines.add("CLAIM_EDITED sheet=" + sheet + " by=" + by + " person=" + person
                            + " amount=" + claim.amount().toPlainString() + " date=" + claim.date());
                }
                claim = claims.find(sheet, claim.id());
            }
            if (ApplyClaimStatus.needsNote(claim, people, rates, settings, LateCharge.of(sheet, sheets, settings, claims))) {
                throw new InvalidClaimException("A claim whose due is above the note threshold needs a note to be opened");
            }
            if (ClaimRules.overCap(claim, periods, claims)) {
                throw new InvalidClaimException("The person already has as many open claims in the period as its cap allows");
            }
            boolean review = ApplyClaimStatus.needsReview(claim, people, rates, settings, LateCharge.of(sheet, sheets, settings, claims));
            String number = null;
            Claim opened = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), review ? ClaimStatus.REVIEW : ClaimStatus.OPEN, number, claim.due(), by, claim.stated(), claim.statedDue(),
                    claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(),
                    claim.ref(), claim.tags(), claim.held());
            if (!claims.updateStatus(opened, ClaimStatus.DRAFT)) {
                throw new InvalidClaimException("The claim was changed by someone else");
            }
            log.write(opened, by);
            lines.add((review ? "CLAIM_REVIEW" : "CLAIM_OPENED") + " sheet=" + sheet + " by=" + by + " person=" + person
                    + " amount=" + opened.amount().toPlainString() + " date=" + opened.date());
            lines.addAll(rollup.lines(sheet, by, before));
        }
        return lines;
    }
}
