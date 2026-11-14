package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Settles a claim, fixing its due: its amount at its person's grade's rate in force on its date, rounded by the sheet's policy date to
 *  two decimals and at least the sheet's floor (a claim bound by a statement: the figure it was bound to). With no rate the due is not
 *  fixed and stays live. Its late charge on the sheet's today is added and fixed with it. Refused for the user who opened it (four eyes), and for a claim
 *  that is not OPEN. Settled by a clerk when the due is above the sheet's clerk limit (a due that is n/a is within any limit; a steward settles
 *  anything), it goes to PENDING instead (after the proof threshold: one above it with no proofs is refused), nothing fixed, until a steward approves or rejects it. */
public class SettleClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        settle(by, claim, people, rates, periods, roles, settings, sheets);
    }

    /** The rules of settling one claim, shared with {@link BulkSettleClaims}: moves the claim (unsaved) or throws. */
    static void settle(String by, Claim claim, PersonRepository people, RateRepository rates,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        if (!ClaimStatus.OPEN.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be settled");
        }
        if (claim.isDeferred()) {
            throw new ClaimStatusException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
        }
        ClaimStatus settled = ClaimStatus.OPEN.moveTo(ClaimStatus.SETTLED);
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by == null ? "" : by)) {
            throw new ClaimStatusException("The user who opened a claim cannot settle it");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()));
        // A claim bound by a statement settles at the figure it was bound to.
        // Its late charge on the sheet's today is frozen in.
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        String due = Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                settings.floor(claim.getSheetId()).on(claim.getDate()), late.rounding(claim.getRoundDate())), late);
        requireProof(claim, due, settings);
        if (due != null && Role.CLERK.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            if (limit != null && new BigDecimal(due).compareTo(limit) > 0) {
                claim.setStatus(ClaimStatus.OPEN.moveTo(ClaimStatus.PENDING).name());
                return;
            }
        }
        claim.setStatus(settled.name());
        claim.setSettledDue(due);
    }

    /** Refuses settling or approving a claim with no proofs whose due (null: n/a, never above) is above
     *  the sheet's proof threshold in force on the claim's date. */
    static void requireProof(Claim claim, String due, SettingRepository settings) throws ClaimStatusException {
        if (due == null || claim.getProofs() > 0) return;
        BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), claim.getDate());
        if (threshold != null && new BigDecimal(due).compareTo(threshold) > 0) {
            throw new ClaimStatusException("A claim whose due is above the proof threshold needs a proof");
        }
    }
}
