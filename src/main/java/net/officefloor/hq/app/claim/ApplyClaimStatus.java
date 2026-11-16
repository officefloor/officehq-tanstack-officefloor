package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new status: DRAFT to OPEN, OPEN to SETTLED (or PENDING), PENDING to SETTLED (approve) or back
 * to OPEN (reject), DRAFT, OPEN or PENDING to VOID. A voided claim keeps
 * its number (never given again). Settling fixes the claim's due: its amount at its
 * person's grade's rate in force on the claim's date, rounded by its rounding (see Rounding) and at least the sheet's floor (left live when no rate applies),
 * or the figure a statement bound it to, with its late charge as of the sheet's today frozen in (see LateCharge).
 * Opening records who opened the claim; that user cannot settle nor approve it (four eyes). A claim dated inside a closed
 * period cannot change status. A clerk (any user not made a steward) settling a claim whose due is above the sheet's
 * clerk limit, if it has one, makes it PENDING instead (its due left live, not bound by a statement); a due of n/a is
 * within any limit. A steward settles anything. Only a steward approves a PENDING claim (fixing its live figure, late
 * charge included, at that moment) or rejects it (OPEN again, bound again by its statement if one bound it).
 */
public class ApplyClaimStatus {
    public void apply(@Val Claim claim, @Val ClaimStatusChange change, PersonRepository people, RateRepository rates,
            PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, ClaimRepository claims, Out<Claim> changed, Out<ClaimNumber> number) throws InvalidClaimStatusException {
        boolean allowed = switch (change.action()) {
            case OPEN -> claim.status() == ClaimStatus.DRAFT;
            case SETTLE -> claim.status() == ClaimStatus.OPEN;
            case APPROVE, REJECT -> claim.status() == ClaimStatus.PENDING;
            case VOID -> claim.status() == ClaimStatus.DRAFT || claim.status() == ClaimStatus.OPEN
                    || claim.status() == ClaimStatus.PENDING;
        };
        if (!allowed) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be made " + change.to());
        }
        if (periods.closedOn(claim.sheetId(), claim.date())) {
            throw new InvalidClaimStatusException("The claim's date is inside a closed period");
        }
        boolean steward = RoleRepository.STEWARD.equals(roles.roleOf(claim.sheetId(), change.by()));
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE)
                && change.by().equals(claim.openedBy())) {
            throw new InvalidClaimStatusException("The user who opened a claim cannot settle nor approve it");
        }
        if ((change.action() == ClaimAction.APPROVE || change.action() == ClaimAction.REJECT) && !steward) {
            throw new InvalidClaimStatusException("Only a steward approves or rejects a pending claim");
        }
        ClaimStatus to = change.to();
        LateCharge late = LateCharge.of(claim.sheetId(), sheets, settings, claims);
        BigDecimal settledDue = to == ClaimStatus.SETTLED ? late.add(claim, due(claim, people, rates, settings, late.rounding())) : claim.due();
        if (change.action() == ClaimAction.SETTLE && settledDue != null && !steward) {
            BigDecimal limit = settings.clerkLimit(claim.sheetId());
            if (limit != null && settledDue.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING;
                settledDue = null;
            }
        }
        number.set(new ClaimNumber(claim.number()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), to, claim.number(), settledDue,
                change.action() == ClaimAction.OPEN ? change.by() : claim.openedBy(), claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy()));
    }

    private static BigDecimal due(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            Rounding rounding) {
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? null : ClaimDue.live(claim, person.gradeOn(claim.date()), rates, settings.floor(claim.sheetId()), rounding);
    }
}
