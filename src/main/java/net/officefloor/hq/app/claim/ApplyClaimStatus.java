package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to the asked status, refusing a change its current status does not allow, an open, settle or void
 * the sheet's permission for it does not give the user's role, an open of a claim with an empty note whose due (as open) is above the
 * sheet's note threshold in force on the claim's date, an open of a claim whose period (by its due-by) caps its person's OPEN or PENDING claims there and they already have that many, and
 * refusing to let the user who opened it, or who last edited it, settle it; a clerk's settle whose due is above the sheet's
 * clerk limit in force on its today leaves it PENDING instead — but first, a settle or approve of a claim without a proof whose due is
 * above the proof threshold in force on the claim's date (its person's grade's on that date when set, else the sheet's) is refused, as is one missing a proof of any of the sheet's proof kinds (a due of n/a is within any limit; a steward settles anything). A review of a
 * PENDING claim is a steward's: approving settles it at its due (the figure a statement binds it to, while one does) (never by the user who opened it or last edited it), rejecting
 * returns it to OPEN, still bound by its last statement if that bound it. A steward may void a SETTLED claim within
 * seven days of its settlement (its status log's last SETTLED date) by the sheet's today. While the sheet has void reasons,
 * a void needs one of them, kept on the claim. Opening records who opened it; a voided claim keeps its number. Settling
 * fixes the claim's due at the rate in force on the claim's date of its person's grade on that date — or, while a statement binds it, at
 * the figure it was bound to — with its late charge as of the sheet's today on top and then at least the sheet's floor in force on the claim's date, frozen in; without a figure its due stays live.
 * An open whose due (as open) is above the review threshold in force on the claim's date leaves it in REVIEW instead,
 * recording who opened it but giving no number; a steward's review then opens it with its number (anyone else is refused).
 * While a steward's flag on the claim's person holds on the sheet's today, only a steward may open it.
 */
public class ApplyClaimStatus {
    // A DRAFT claim may be deleted, and a DELETED one restored to DRAFT only on the sheet-today it was deleted.
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods, ClaimLogRepository log, @Val ChangedBy by, @Val ClaimReview review, @Val VoidChoice voidChoice, VoidReasonRepository voidReasons,
            Out<ClaimNumber> audited) throws BadRequestException {
        change(status, claim, repository, people, rates, roles, settings, sheets, periods, log, by, review, voidChoice,
                voidReasons, audited, false);
    }

    /**
     * The change itself; with {@code judged}, a settle whose four-eyes rules and proof rule were already judged on another
     * claim (a part-settle's original) and are not judged again on this one.
     */
    void change(ClaimStatus status, Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods, ClaimLogRepository log, ChangedBy by, ClaimReview review, VoidChoice voidChoice, VoidReasonRepository voidReasons,
            Out<ClaimNumber> audited, boolean judged) throws BadRequestException {
        ClaimStatus from = claim.getStatus();
        String role = roles.roleOf(claim.getSheetId(), by.name());
        boolean steward = Role.STEWARD.equals(role);
        if (review.release()) {
            if (from != ClaimStatus.REVIEW) {
                throw new BadRequestException("Only a REVIEW claim is reviewed; this one is " + from);
            }
            if (!steward) {
                throw new BadRequestException("Only a steward reviews a claim");
            }
            claim.setStatus(ClaimStatus.OPEN);
            if (!repository.updateStatus(claim, from)) {
                throw new BadRequestException("The claim was changed by someone else; reload the sheet");
            }
            claim.setNumber(number(claim, repository, settings, sheets, periods));
            repository.updateStatus(claim, ClaimStatus.OPEN);
            audited.set(new ClaimNumber(claim.getNumber()));
            return;
        }
        if (review.asked()) {
            if (from != ClaimStatus.PENDING) {
                throw new BadRequestException("Only a PENDING claim is approved or rejected; this one is " + from);
            }
            if (!steward) {
                throw new BadRequestException("Only a steward approves or rejects a claim");
            }
            if (status == ClaimStatus.SETTLED && by.name().equals(claim.getOpenedBy())) {
                throw new BadRequestException("The user who opened a claim cannot approve it");
            }
            if (status == ClaimStatus.SETTLED && by.name().equals(claim.getEditedBy())) {
                throw new BadRequestException("The user who last edited a claim cannot approve it");
            }
        } else {
            if (!from.canBecome(status)) {
                throw new BadRequestException(status == ClaimStatus.DELETED ? "Only a DRAFT claim can be deleted"
                        : status == ClaimStatus.DRAFT ? "Only a DELETED claim can be restored"
                        : "A " + from + " claim cannot become " + status);
            }
            if (status == ClaimStatus.DRAFT) {
                java.time.LocalDate deletedOn = log.lastOn(claim.getSheetId(), claim.getId(), ClaimStatus.DELETED);
                if (deletedOn == null || !deletedOn.equals(sheets.today(claim.getSheetId()))) {
                    throw new BadRequestException("A deleted claim can be restored only on the day it was deleted");
                }
            }
            if (status == ClaimStatus.DELETED || status == ClaimStatus.DRAFT) {
                claim.setStatus(status);
                if (!repository.updateStatus(claim, from)) {
                    throw new BadRequestException("The claim was changed by someone else; reload the sheet");
                }
                audited.set(new ClaimNumber(claim.getNumber()));
                return;
            }
            String action = status == ClaimStatus.OPEN ? "open" : status == ClaimStatus.VOID ? "void" : "settle";
            if (!settings.allows(claim.getSheetId(), action, role)) {
                throw new BadRequestException("A " + role + " may not " + action + " a claim on this sheet");
            }
            if (status == ClaimStatus.OPEN && !steward) {
                Person owner = people.find(claim.getSheetId(), claim.getPersonId());
                if (owner != null && owner.isFlaggedOn(sheets.today(claim.getSheetId()))) {
                    throw new BadRequestException("This person is flagged (" + owner.getFlagReason()
                            + "); only a steward may open their claims until " + owner.getFlagUntil());
                }
            }
            if (status == ClaimStatus.VOID && from == ClaimStatus.SETTLED) {
                if (!steward) {
                    throw new BadRequestException("Only a steward voids a SETTLED claim");
                }
                java.time.LocalDate settledOn = log.lastOn(claim.getSheetId(), claim.getId(), ClaimStatus.SETTLED);
                java.time.LocalDate today = sheets.today(claim.getSheetId());
                if (settledOn == null || today.isAfter(settledOn.plusDays(ClaimStatus.VOID_SETTLED_DAYS))) {
                    throw new BadRequestException("A SETTLED claim is voided only within "
                            + ClaimStatus.VOID_SETTLED_DAYS + " days of its settlement");
                }
            }
            if (status == ClaimStatus.VOID) {
                java.util.List<String> reasons = voidReasons.list(claim.getSheetId());
                if (!reasons.isEmpty() && voidChoice.reason() == null) {
                    throw new BadRequestException("Choose a void reason from the sheet's list");
                }
                if (voidChoice.reason() != null && !reasons.contains(voidChoice.reason())) {
                    throw new BadRequestException("That void reason is not on the sheet's list");
                }
            }
            if (status == ClaimStatus.SETTLED && !judged) {
                refuseFourEyes(claim, by);
            }
            if (status == ClaimStatus.SETTLED && claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
                throw new BadRequestException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
            }
        }
        LateCharge late = settings.lateCharge(claim.getSheetId(), sheets.today(claim.getSheetId()));
        BigDecimal due = status == ClaimStatus.SETTLED ? late.due(claim, dueNow(claim, people, rates, settings, late)) : null;
        if (status == ClaimStatus.OPEN && !review.asked() && (claim.getNote() == null || claim.getNote().isBlank())) {
            BigDecimal threshold = late.settings().noteThreshold(claim.getDate());
            BigDecimal opened = threshold == null ? null : late.dueAsOpen(claim, dueNow(claim, people, rates, settings, late));
            if (opened != null && opened.compareTo(threshold) > 0) {
                throw new BadRequestException("A claim whose due is above the note threshold needs a note before it is opened");
            }
        }
        if (status == ClaimStatus.OPEN && !review.asked()) {
            refuseOverCap(claim, repository, periods);
        }
        if (status == ClaimStatus.SETTLED && due != null && !judged) {
            refuseWithoutProof(claim, due, people, settings, late);
        }
        ClaimStatus to = status;
        if (!review.asked() && status == ClaimStatus.SETTLED && due != null && !steward) {
            BigDecimal limit = late.settings().clerkLimit(late.today());
            if (limit != null && due.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING; // awaits a steward's approval
            }
        }
        if (status == ClaimStatus.OPEN && !review.asked()) {
            BigDecimal threshold = late.settings().reviewThreshold(claim.getDate());
            BigDecimal opened = threshold == null ? null : late.dueAsOpen(claim, dueNow(claim, people, rates, settings, late));
            if (opened != null && opened.compareTo(threshold) > 0) {
                to = ClaimStatus.REVIEW; // awaits a steward's review, numbered only then
            }
        }
        claim.setStatus(to);
        if (to == ClaimStatus.VOID) {
            claim.setVoidReason(voidChoice.reason());
        }
        boolean opening = to == ClaimStatus.OPEN && !review.asked();
        if (opening || to == ClaimStatus.REVIEW) {
            claim.setOpenedBy(by.name());
        }
        if (to == ClaimStatus.SETTLED) {
            // A voided SETTLED claim keeps its frozen figure (shown, and reversed in its audit) but counts in no total.
            claim.setSettledDue(due);
        }
        if (!repository.updateStatus(claim, from)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (opening) {
            // Numbered only once the claim is surely moving, so a refused open uses up no number.
            claim.setNumber(number(claim, repository, settings, sheets, periods));
            repository.updateStatus(claim, to);
        }
        audited.set(new ClaimNumber(claim.getNumber()));
    }

    /** Refuses the settle of a claim by the user who opened it or who last edited it. */
    static void refuseFourEyes(Claim claim, ChangedBy by) throws BadRequestException {
        if (by.name().equals(claim.getOpenedBy())) {
            throw new BadRequestException("The user who opened a claim cannot settle it");
        }
        if (by.name().equals(claim.getEditedBy())) {
            throw new BadRequestException("The user who last edited a claim cannot settle it");
        }
    }

    /**
     * Refuses the settle at {@code due} of a claim without a proof (or missing one of the sheet's proof kinds) when the due
     * is above the proof threshold in force on the claim's date (its person's grade's on that date when set, else the sheet's).
     */
    static void refuseWithoutProof(Claim claim, BigDecimal due, PersonRepository people, SettingRepository settings,
            LateCharge late) throws BadRequestException {
        java.util.List<String> kinds = settings.proofKinds(claim.getSheetId());
        String missing = kinds.stream().filter(k -> claim.proofsOf(k) == 0).findFirst().orElse(null);
        if (claim.getProofs() == 0 || missing != null) {
            Person owner = people.find(claim.getSheetId(), claim.getPersonId());
            BigDecimal threshold = late.settings().proofThreshold(owner == null ? null : owner.gradeOn(claim.getDate()),
                    claim.getDate());
            if (threshold != null && due.compareTo(threshold) > 0) {
                throw new BadRequestException(claim.getProofs() == 0
                        ? "A claim whose due is above the proof threshold needs a proof before it is settled"
                        : "A claim whose due is above the proof threshold needs a " + missing + " proof before it is settled");
            }
        }
    }

    /** The due the claim would be settled at now (null: n/a). */
    static BigDecimal settleDue(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            LateCharge late) {
        return late.due(claim, dueNow(claim, people, rates, settings, late));
    }

    /**
     * Refuses opening a claim belonging to a period (by its due-by) with a cap when its person already has that many
     * OPEN or PENDING claims in it.
     */
    static void refuseOverCap(Claim claim, ClaimRepository repository, PeriodRepository periods) throws BadRequestException {
        Period period = periods.on(claim.getSheetId(), claim.getDueBy());
        if (period == null || period.cap() == null) {
            return;
        }
        long held = repository.listOf(claim.getSheetId(), claim.getPersonId()).stream()
                .filter(c -> c.getId() != claim.getId()
                        && (c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING)
                        && c.getDueBy() != null && !c.getDueBy().isBefore(period.from()) && !c.getDueBy().isAfter(period.to()))
                .count();
        if (held >= period.cap()) {
            throw new BadRequestException("This person already has " + period.cap() + " open claims in period "
                    + period.name());
        }
    }

    /**
     * The number an opened claim takes: while the sheet numbers by period and its today is on or after that setting's
     * date, the next within the claim's period (by its due-by) as {@code <period name>-<n>}, or {@code X-<n>} in no period;
     * otherwise the sheet's next.
     */
    static String number(Claim claim, ClaimRepository repository, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods) {
        return ClaimNumbering.number(claim.getSheetId(), claim.getDueBy(), repository, settings, sheets, periods);
    }

    /** The figure a statement binds the claim to, else the claim's amount times its person's grade's rate on the claim's date, rounded as the claim rounds by the sheet's policy date (not floored); null when there is no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, LateCharge late) {
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                claim.getDate(), claim.getAddedSeq());
        return claim.figureAt(rate, late.rounding(claim));
    }
}
