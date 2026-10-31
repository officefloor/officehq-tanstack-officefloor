package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log, Claims claims,
            net.officefloor.hq.app.asof.AsOfView asOfView, Out<ClaimNumber> number) throws ClaimStatusRefusedException {
        Claim then = null;
        if (request.on() != null) {
            // Settled as of a date: on or before the sheet's today, and only a claim that was on the sheet by its end.
            if (request.on().isAfter(sheets.today(claim.getSheetId()))) {
                throw new ClaimStatusRefusedException("A claim cannot be settled as of a date after the sheet's today");
            }
            then = asOfView.claim(claim.getSheetId(), claim.getId(), request.on());
            if (then == null) {
                throw new ClaimStatusRefusedException("The claim was not on the sheet by " + request.on());
            }
        }
        if (request.target() == ClaimStatus.OPEN && (request.decision() == ValidClaimStatus.Decision.NONE
                && claim.getStatus().canBecome(ClaimStatus.OPEN)
                || request.decision() == ValidClaimStatus.Decision.REVIEW && claim.getStatus() == ClaimStatus.REVIEW)) {
            refuseOverCap(claim, periods, claims.list(claim.getSheetId()));
        }
        ClaimNumber changed = change(request, claim, sheets, people, rates, periods, settings, numbering, log);
        if (then != null && claim.getStatus() == ClaimStatus.SETTLED) {
            // The frozen figure is the claim's due as it stood at the end of the date, as the as-of view works it out.
            claim.setSettledDue(asOfView.due(then, request.on()));
        }
        number.set(changed);
    }

    /** Applies a status change to the claim by the rules of a single change, returning its number;
     *  throws, leaving the claim as it is, when the change is refused. Bulk settle uses it too. */
    static ClaimNumber change(ValidClaimStatus request, Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log)
            throws ClaimStatusRefusedException {
        return change(request, claim, sheets, people, rates, periods, settings, numbering, log, true);
    }

    /** As a single change; without {@code ownRules} the four-eyes and proof rules are left out, as a part-settle
     *  judges them on the original claim instead (see {@link #refuseSettleRules}). */
    static ClaimNumber change(ValidClaimStatus request, Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log, boolean ownRules)
            throws ClaimStatusRefusedException {
        RoundingMode rounding = Rounding.of(claim, settings.policyDate(claim.getSheetId()), on -> settings.roundingMode(claim.getSheetId(), on));
        if (periods.isClosed(claim.getSheetId(), claim.getPeriodDate())) {
            throw new ClaimStatusRefusedException("No claim whose due-by is inside a closed period can change status");
        }
        boolean steward = Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()));
        if (request.decision() == ValidClaimStatus.Decision.NONE) {
            // Opening, settling and voiding are each taken only by the roles the sheet's permission allows.
            String action = switch (request.target()) {
                case OPEN -> "open";
                case SETTLED -> "settle";
                case DELETED -> "delete";
                case DRAFT -> "restore";
                default -> "void";
            };
            if (!settings.allows(claim.getSheetId(), request.user(), action)) {
                throw new ClaimStatusRefusedException("Your role may not " + action + " a claim on this sheet");
            }
        }
        if (request.decision() == ValidClaimStatus.Decision.REVIEW) {
            // Only a steward reviews, and only a REVIEW claim.
            if (claim.getStatus() != ClaimStatus.REVIEW) {
                throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
            }
            if (!steward) {
                throw new ClaimStatusRefusedException("Only a steward can review a REVIEW claim");
            }
        } else if (request.decision() != ValidClaimStatus.Decision.NONE) {
            // Only a steward, or the person's approver, approves or rejects, and only a PENDING claim.
            if (claim.getStatus() != ClaimStatus.PENDING) {
                throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
            }
            String approver = people.find(claim.getSheetId(), claim.getPersonId()).getApprover();
            if (!steward && (approver == null || !approver.equals(request.user()))) {
                throw new ClaimStatusRefusedException("Only a steward or the person's approver can approve or reject a PENDING claim");
            }
        } else if (!claim.getStatus().canBecome(request.target())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
        }
        if (request.target() == ClaimStatus.DRAFT) {
            // A deleted claim is restored only on the same sheet-today it was deleted.
            java.time.LocalDate deletedOn = log.deletedOn(claim);
            if (deletedOn == null || !deletedOn.equals(sheets.today(claim.getSheetId()))) {
                throw new ClaimStatusRefusedException("A deleted claim can be restored only on the sheet-today it was deleted");
            }
        }
        if (ownRules && request.target() == ClaimStatus.SETTLED) {
            refuseSettleRules(claim, request.user(), sheets, people, rates, settings, rounding);
        }
        if (request.target() == ClaimStatus.SETTLED && claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusRefusedException("A deferred claim cannot be settled until its deferred-until date");
        }
        if (request.target() == ClaimStatus.VOID && claim.getStatus() == ClaimStatus.SETTLED) {
            // Only a steward voids a SETTLED claim, and only within 7 days of its settlement by the sheet's today.
            if (!steward) {
                throw new ClaimStatusRefusedException("Only a steward can void a SETTLED claim");
            }
            LocalDate settledOn = log.settledOn(claim);
            if (settledOn == null || sheets.today(claim.getSheetId()).isAfter(settledOn.plusDays(7))) {
                throw new ClaimStatusRefusedException("A SETTLED claim can be voided only within 7 days of its settlement");
            }
        }
        ClaimStatus target = request.target();
        if (target == ClaimStatus.OPEN && request.decision() == ValidClaimStatus.Decision.NONE) {
            refuseFlagged(claim, sheets, people, steward);
            refuseNoteless(claim, sheets, people, rates, settings, rounding);
            if (needsReview(claim, sheets, people, rates, settings, rounding)) {
                // Above the review threshold the claim awaits a steward's review, unnumbered; who opened it is kept.
                claim.setOpenedBy(request.user());
                claim.setStatus(ClaimStatus.REVIEW);
                return new ClaimNumber(claim.getNumber());
            }
        }
        if (target == ClaimStatus.SETTLED && request.decision() == ValidClaimStatus.Decision.NONE && !steward) {
            // A clerk settles only within the clerk limit in force (the person's grade's own, else the sheet's),
            // judged on the claim's due as shown; a due of n/a is within any limit.
            BigDecimal limit = settings.clerkLimitFor(claim.getSheetId(),
                    people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(sheets.today(claim.getSheetId())));
            BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                // Above the clerk limit the claim awaits a steward's approval.
                target = ClaimStatus.PENDING;
            }
        }
        if (target == ClaimStatus.OPEN && request.decision() == ValidClaimStatus.Decision.REVIEW) {
            // Reviewed: numbered as it opens; its opener stays the user who opened it.
            claim.setNumber(numbering.next(claim.getSheetId(), claim.getPeriodDate()));
        }
        if (target == ClaimStatus.OPEN && request.decision() == ValidClaimStatus.Decision.NONE) {
            claim.setNumber(numbering.next(claim.getSheetId(), claim.getPeriodDate()));
            claim.setOpenedBy(request.user());
        }
        // A voided claim keeps its number; numbers are never reused.
        ClaimNumber number = new ClaimNumber(claim.getNumber());
        if (target == ClaimStatus.SETTLED) {
            // Settling (or approving) fixes the due: the figure a statement bound it to, else its amount
            // at the rate in force on the claim's date, with its late charge as of today on top and raised to the floor, frozen in;
            // with no figure it stays live (n/a until a rate is set).
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            BigDecimal figure = claim.isBound() && claim.getStatedDue() != null ? claim.getStatedDue()
                    : Rates.due(claim.getAmount(), rates.find(claim, grade), rounding);
            claim.setSettledDue(LateCharge.due(claim, figure, settings.floor(claim.getSheetId(), claim.getDate()),
                    on -> settings.latePct(claim.getSheetId(), on), sheets.today(claim.getSheetId()), rounding,
                    settings.grace(claim.getSheetId(), grade, claim.getDate())));
        }
        // A rejected claim is OPEN again, so bound again by the last statement if it included it.
        if (target == ClaimStatus.VOID) {
            claim.setVoidReason(request.reason());
        }
        claim.setStatus(target);
        return number;
    }

    /** Refuses settling (or approving) the claim by the user under the four-eyes rules and the proof rule.
     *  A part-settle judges them on the original claim. */
    static void refuseSettleRules(Claim claim, String user, Sheets sheets, People people, Rates rates, Settings settings,
            RoundingMode rounding) throws ClaimStatusRefusedException {
        if (claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(user)) {
            // Four eyes: the user who opened a claim cannot settle (or approve) it.
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        if (claim.getEditedBy() != null
                && claim.getEditedBy().equals(user)) {
            // Four eyes: nor can the user who last edited it.
            throw new ClaimStatusRefusedException("The user who last edited a claim cannot settle it");
        }
        java.util.List<String> kinds = settings.proofKinds(claim.getSheetId());
        if (claim.getProofs() == 0 || kinds.stream().anyMatch(k -> claim.proofsOf(k) == 0)) {
            // Above the proof threshold in force on the claim's date a claim needs a proof to be settled or approved,
            // judged on its due as shown (before the clerk limit is considered); a due of n/a is within any threshold.
            // The person's grade's own threshold in force then, when one is set, takes the place of the sheet's.
            // While the sheet's proof kinds are set, it needs at least one proof of each listed kind.
            BigDecimal threshold = settings.proofThresholdFor(claim.getSheetId(),
                    people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate()), claim.getDate());
            BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
            if (threshold != null && due != null && due.compareTo(threshold) > 0) {
                throw new ClaimStatusRefusedException("A claim whose due is above the proof threshold needs a proof to be settled");
            }
        }
    }

    /** Refuses opening a claim that belongs to a capped period (by its period date) when its person already has
     *  that many OPEN or PENDING claims in the period, counted among the sheet's claims. Bulk open uses it too. */
    static void refuseOverCap(Claim claim, Periods periods, List<Claim> all) throws ClaimStatusRefusedException {
        PeriodResponse period = periods.cappedOn(claim.getSheetId(), claim.getPeriodDate());
        if (period == null) {
            return;
        }
        LocalDate from = LocalDate.parse(period.from());
        LocalDate to = LocalDate.parse(period.to());
        long held = all.stream()
                .filter(c -> c.getId() != claim.getId() && c.getPersonId() == claim.getPersonId())
                .filter(c -> c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING)
                .filter(c -> !c.getPeriodDate().isBefore(from) && !c.getPeriodDate().isAfter(to))
                .count();
        if (held >= Integer.parseInt(period.cap())) {
            throw new ClaimStatusRefusedException("The person already has " + period.cap()
                    + " OPEN or PENDING claims in period " + period.name() + ", its cap");
        }
    }

    /** Refuses opening a claim of a person whose flag is in force on the sheet's today, unless by a steward.
     *  Bulk open uses it too. */
    static void refuseFlagged(Claim claim, Sheets sheets, People people, boolean steward) throws ClaimStatusRefusedException {
        if (!steward && people.find(claim.getSheetId(), claim.getPersonId()).flaggedOn(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusRefusedException("Only a steward can open a claim of a flagged person");
        }
    }

    /** Refuses opening a claim with an empty note whose due as shown is above the note threshold in force on its date;
     *  a due of n/a is within any threshold. Bulk open uses it too. */
    static void refuseNoteless(Claim claim, Sheets sheets, People people, Rates rates, Settings settings,
            RoundingMode rounding) throws ClaimStatusRefusedException {
        if (claim.getNote() != null && !claim.getNote().trim().isEmpty()) {
            return;
        }
        BigDecimal threshold = settings.noteThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) {
            return;
        }
        BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
        if (due != null && due.compareTo(threshold) > 0) {
            throw new ClaimStatusRefusedException("A claim whose due is above the note threshold needs a note to be opened");
        }
    }

    /** Whether opening the claim sends it to REVIEW: its due as shown is above the review threshold in force on its
     *  date; a due of n/a is within any threshold. Bulk open uses it too. */
    static boolean needsReview(Claim claim, Sheets sheets, People people, Rates rates, Settings settings,
            RoundingMode rounding) {
        BigDecimal threshold = settings.reviewThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) {
            return false;
        }
        BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
        return due != null && due.compareTo(threshold) > 0;
    }

    /** The claim's due as shown: fixed (settled or bound), else its live figure with its late charge as of today; null for n/a. */
    private static BigDecimal shownDue(Claim claim, Sheets sheets, People people, Rates rates, Settings settings,
            RoundingMode rounding) {
        String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
        BigDecimal fixed = ClaimResponse.fixedDue(claim);
        BigDecimal figure = fixed != null || claim.isBound() ? fixed
                : Rates.due(claim.getAmount(), rates.find(claim, grade), rounding);
        return LateCharge.due(claim, figure, settings.floor(claim.getSheetId(), claim.getDate()),
                on -> settings.latePct(claim.getSheetId(), on), sheets.today(claim.getSheetId()), rounding,
                settings.grace(claim.getSheetId(), grade, claim.getDate()));
    }
}
