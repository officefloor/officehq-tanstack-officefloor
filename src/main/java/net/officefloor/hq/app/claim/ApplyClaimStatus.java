package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Opening records who opened the claim; that user cannot settle nor approve it (four eyes). A claim whose due is
 * above the sheet's proof threshold in force on its date (if it has one) cannot be settled nor approved while its proofs
 * count is 0 (checked before the clerk limit, so it never goes PENDING); a due of n/a is within any threshold. A claim whose due-by falls inside a closed
 * period cannot change status, nor can a deferred claim be settled (see ClaimDue#deferred). A clerk (any user not made a steward) settling a claim whose due is above the sheet's
 * clerk limit, if it has one, makes it PENDING instead (its due left live, not bound by a statement); a due of n/a is
 * within any limit. A steward settles anything. Only a steward approves a PENDING claim (fixing its live figure, late
 * charge included, at that moment) or rejects it (OPEN again, bound again by its statement if one bound it).
 * A claim whose due (as it shows: its live figure at least the floor, or the figure a statement bound it to) is above
 * the sheet's note threshold in force on its date (if it has one) cannot be opened while its note is empty; a due of
 * n/a is within any threshold (see #needsNote).
 * Opening, settling and voiding are each taken only by the roles the sheet's permission allows (see SettingRepository#permits).
 * Only a steward voids a SETTLED claim, and only within 7 days of its settlement (the sheet's today on its status log's
 * latest SETTLED line) by the sheet's today; its frozen figure is kept on it but, VOID, it leaves every total.
 * Only a steward re-settles a SETTLED claim (not one whose due-by falls inside a closed period): its due is fixed again at the figure
 * it would have today as an OPEN claim bound by no statement (today's rates, grades, floor and late charge).
 */
public class ApplyClaimStatus {

    /** How many days after its settlement a steward may still void a SETTLED claim. */
    static final int VOID_SETTLED_DAYS = 7;

    public void apply(@Val Claim claim, @Val ClaimStatusChange change, PersonRepository people, RateRepository rates,
            PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, ClaimRepository claims, ClaimLog log, Out<Claim> changed, Out<ClaimNumber> number, Out<PreviousDue> previous) throws InvalidClaimStatusException {
        boolean allowed = switch (change.action()) {
            case OPEN -> claim.status() == ClaimStatus.DRAFT;
            case SETTLE -> claim.status() == ClaimStatus.OPEN;
            case APPROVE, REJECT -> claim.status() == ClaimStatus.PENDING;
            case VOID -> claim.status() == ClaimStatus.DRAFT || claim.status() == ClaimStatus.OPEN
                    || claim.status() == ClaimStatus.PENDING || claim.status() == ClaimStatus.SETTLED;
            case RESETTLE -> claim.status() == ClaimStatus.SETTLED;
        };
        if (!allowed) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be made " + change.to());
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        if (change.action() == ClaimAction.SETTLE && ClaimDue.deferred(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("A deferred claim cannot be settled until the date it is deferred until");
        }
        String role = roles.roleOf(claim.sheetId(), change.by());
        boolean steward = RoleRepository.STEWARD.equals(role);
        String permission = switch (change.action()) {
            case OPEN -> "open";
            case SETTLE -> "settle";
            case VOID -> "void";
            default -> null;
        };
        if (permission != null && !settings.permits(claim.sheetId(), permission, role)) {
            throw new InvalidClaimStatusException("A " + role + " may not " + permission + " a claim");
        }
        if (change.action() == ClaimAction.OPEN && needsNote(claim, people, rates, settings,
                LateCharge.of(claim.sheetId(), sheets, settings, claims))) {
            throw new InvalidClaimStatusException("A claim whose due is above the note threshold needs a note to be opened");
        }
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE)
                && change.by().equals(claim.openedBy())) {
            throw new InvalidClaimStatusException("The user who opened a claim cannot settle nor approve it");
        }
        if ((change.action() == ClaimAction.APPROVE || change.action() == ClaimAction.REJECT) && !steward) {
            throw new InvalidClaimStatusException("Only a steward approves or rejects a pending claim");
        }
        if (change.action() == ClaimAction.VOID && claim.status() == ClaimStatus.SETTLED) {
            if (!steward) {
                throw new InvalidClaimStatusException("Only a steward voids a settled claim");
            }
            LocalDate settledOn = log.settledOn(claim);
            if (settledOn == null || sheets.today(claim.sheetId()).isAfter(settledOn.plusDays(VOID_SETTLED_DAYS))) {
                throw new InvalidClaimStatusException("A settled claim can only be voided within "
                        + VOID_SETTLED_DAYS + " days of its settlement");
            }
        }
        if (change.action() == ClaimAction.RESETTLE && !steward) {
            throw new InvalidClaimStatusException("Only a steward re-settles a claim");
        }
        ClaimStatus to = change.to();
        LateCharge late = LateCharge.of(claim.sheetId(), sheets, settings, claims);
        BigDecimal settledDue;
        if (change.action() == ClaimAction.RESETTLE) {
            Claim asOpen = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), ClaimStatus.OPEN, claim.number(), null, claim.openedBy(), false, null,
                    claim.corrects(), claim.dueBy(), null, claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags());
            settledDue = late.add(asOpen, due(asOpen, people, rates, late.rounding()), settings.floor(claim.sheetId(), claim.date()));
        } else {
            settledDue = to == ClaimStatus.SETTLED ? late.add(claim, due(claim, people, rates, late.rounding()), settings.floor(claim.sheetId(), claim.date())) : claim.due();
        }
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE) && settledDue != null
                && claim.proofs() == 0) {
            BigDecimal threshold = settings.proofThreshold(claim.sheetId(), claim.date());
            if (threshold != null && settledDue.compareTo(threshold) > 0) {
                throw new InvalidClaimStatusException("A claim whose due is above the proof threshold needs a proof");
            }
        }
        if (change.action() == ClaimAction.SETTLE && settledDue != null && !steward) {
            BigDecimal limit = settings.clerkLimit(claim.sheetId());
            if (limit != null && settledDue.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING;
                settledDue = null;
            }
        }
        number.set(new ClaimNumber(claim.number()));
        previous.set(new PreviousDue(claim.due(), claim.status()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), to, claim.number(), settledDue,
                change.action() == ClaimAction.OPEN ? change.by() : claim.openedBy(), claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags()));
    }

    /** Whether the claim cannot be opened for want of a note: its note is empty and its due is above the sheet's note
     *  threshold in force on its date (none: never; a due of n/a: never). */
    static boolean needsNote(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            LateCharge late) {
        if (claim.note() != null && !claim.note().isBlank()) {
            return false;
        }
        BigDecimal threshold = settings.noteThreshold(claim.sheetId(), claim.date());
        if (threshold == null) {
            return false;
        }
        BigDecimal due = late.add(claim, due(claim, people, rates, late.rounding()),
                settings.floor(claim.sheetId(), claim.date()));
        return due != null && due.compareTo(threshold) > 0;
    }

    private static BigDecimal due(Claim claim, PersonRepository people, RateRepository rates, Rounding rounding) {
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? null : ClaimDue.live(claim, person.gradeOn(claim.date()), rates, rounding);
    }
}
