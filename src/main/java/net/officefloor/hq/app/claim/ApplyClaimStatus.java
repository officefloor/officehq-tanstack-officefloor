package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        RoundingMode rounding = Rounding.of(claim, settings.policyDate(claim.getSheetId()));
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can change status");
        }
        boolean steward = Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()));
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
        ClaimStatus target = request.target();
        if (target == ClaimStatus.SETTLED && request.decision() == ValidClaimStatus.Decision.NONE && !steward) {
            // A clerk settles only within the sheet's clerk limit, judged on the claim's due as shown;
            // a due of n/a is within any limit.
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            BigDecimal fixed = ClaimResponse.fixedDue(claim);
            BigDecimal figure = fixed != null || claim.isBound() ? fixed
                    : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()),
                    settings.floor(claim.getSheetId()), rounding);
            BigDecimal due = LateCharge.withCharge(claim, figure, settings.latePct(claim.getSheetId()),
                    sheets.today(claim.getSheetId()), rounding);
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
        number.set(new ClaimNumber(claim.getNumber()));
        if (target == ClaimStatus.SETTLED) {
            // Settling (or approving) fixes the due: the figure a statement bound it to (never while PENDING), else its amount (raised to the floor)
            // at the rate in force on the claim's date, with its late charge as of today frozen in; with no
            // figure it stays live (n/a until a rate is set).
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            BigDecimal figure = claim.isBound() && claim.getStatedDue() != null ? claim.getStatedDue()
                    : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()),
                    settings.floor(claim.getSheetId()), rounding);
            claim.setSettledDue(LateCharge.withCharge(claim, figure, settings.latePct(claim.getSheetId()),
                    sheets.today(claim.getSheetId()), rounding));
        }
        // A rejected claim is OPEN again, so bound again by the last statement if it included it.
        claim.setStatus(target);
    }
}
