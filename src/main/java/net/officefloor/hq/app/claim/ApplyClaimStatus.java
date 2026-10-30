package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        number.set(change(request, claim, sheets, people, rates, periods, settings, numbering, log));
    }

    /** Applies a status change to the claim by the rules of a single change, returning its number;
     *  throws, leaving the claim as it is, when the change is refused. Bulk settle uses it too. */
    static ClaimNumber change(ValidClaimStatus request, Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log)
            throws ClaimStatusRefusedException {
        RoundingMode rounding = Rounding.of(claim, settings.policyDate(claim.getSheetId()));
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can change status");
        }
        boolean steward = Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()));
        if (request.decision() == ValidClaimStatus.Decision.NONE) {
            // Opening, settling and voiding are each taken only by the roles the sheet's permission allows.
            String action = switch (request.target()) {
                case OPEN -> "open";
                case SETTLED -> "settle";
                default -> "void";
            };
            if (!settings.allows(claim.getSheetId(), request.user(), action)) {
                throw new ClaimStatusRefusedException("Your role may not " + action + " a claim on this sheet");
            }
        }
        if (request.decision() != ValidClaimStatus.Decision.NONE) {
            // Only a steward approves or rejects, and only a PENDING claim.
            if (claim.getStatus() != ClaimStatus.PENDING) {
                throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
            }
            if (!steward) {
                throw new ClaimStatusRefusedException("Only a steward can approve or reject a PENDING claim");
            }
        } else if (!claim.getStatus().canBecome(request.target())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
        }
        if (request.target() == ClaimStatus.SETTLED && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(request.user())) {
            // Four eyes: the user who opened a claim cannot settle (or approve) it.
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
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
            refuseNoteless(claim, sheets, people, rates, settings, rounding);
        }
        if (target == ClaimStatus.SETTLED && claim.getProofs() == 0) {
            // Above the proof threshold in force on the claim's date a claim needs a proof to be settled or approved,
            // judged on its due as shown (before the clerk limit is considered); a due of n/a is within any threshold.
            BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), claim.getDate());
            BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
            if (threshold != null && due != null && due.compareTo(threshold) > 0) {
                throw new ClaimStatusRefusedException("A claim whose due is above the proof threshold needs a proof to be settled");
            }
        }
        if (target == ClaimStatus.SETTLED && request.decision() == ValidClaimStatus.Decision.NONE && !steward) {
            // A clerk settles only within the sheet's clerk limit, judged on the claim's due as shown;
            // a due of n/a is within any limit.
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            BigDecimal due = shownDue(claim, sheets, people, rates, settings, rounding);
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                // Above the clerk limit the claim awaits a steward's approval.
                target = ClaimStatus.PENDING;
            }
        }
        if (target == ClaimStatus.OPEN && request.decision() == ValidClaimStatus.Decision.NONE) {
            claim.setNumber(numbering.next(claim.getSheetId(), claim.getDate()));
            claim.setOpenedBy(request.user());
        }
        // A voided claim keeps its number; numbers are never reused.
        ClaimNumber number = new ClaimNumber(claim.getNumber());
        if (target == ClaimStatus.SETTLED) {
            // Settling (or approving) fixes the due: the figure a statement bound it to (never while PENDING), else its amount
            // at the rate in force on the claim's date, with its late charge as of today on top and raised to the floor, frozen in;
            // with no figure it stays live (n/a until a rate is set).
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            BigDecimal figure = claim.isBound() && claim.getStatedDue() != null ? claim.getStatedDue()
                    : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()), rounding);
            claim.setSettledDue(LateCharge.due(claim, figure, settings.floor(claim.getSheetId(), claim.getDate()),
                    on -> settings.latePct(claim.getSheetId(), on), sheets.today(claim.getSheetId()), rounding));
        }
        // A rejected claim is OPEN again, so bound again by the last statement if it included it.
        claim.setStatus(target);
        return number;
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

    /** The claim's due as shown: fixed (settled or bound), else its live figure with its late charge as of today; null for n/a. */
    private static BigDecimal shownDue(Claim claim, Sheets sheets, People people, Rates rates, Settings settings,
            RoundingMode rounding) {
        String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
        BigDecimal fixed = ClaimResponse.fixedDue(claim);
        BigDecimal figure = fixed != null || claim.isBound() ? fixed
                : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()), rounding);
        return LateCharge.due(claim, figure, settings.floor(claim.getSheetId(), claim.getDate()),
                on -> settings.latePct(claim.getSheetId(), on), sheets.today(claim.getSheetId()), rounding);
    }
}
