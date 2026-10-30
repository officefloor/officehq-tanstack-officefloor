package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can change status");
        }
        if (!claim.getStatus().canBecome(request.target())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
        }
        if (request.target() == ClaimStatus.SETTLED && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(request.user())) {
            // Four eyes: the user who opened a claim cannot settle it.
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        if (request.target() == ClaimStatus.SETTLED
                && !Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()))) {
            // A clerk settles only within the sheet's clerk limit, judged on the claim's due as shown;
            // a due of n/a is within any limit.
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            BigDecimal fixed = ClaimResponse.fixedDue(claim);
            BigDecimal figure = fixed != null || claim.isStated() ? fixed
                    : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()),
                    settings.floor(claim.getSheetId()));
            BigDecimal due = LateCharge.withCharge(claim, figure, settings.latePct(claim.getSheetId()),
                    settings.terms(claim.getSheetId()), sheets.today(claim.getSheetId()));
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                throw new ClaimStatusRefusedException("A clerk cannot settle a claim whose due is above the clerk limit");
            }
        }
        if (request.target() == ClaimStatus.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
            claim.setOpenedBy(request.user());
        }
        // A voided claim keeps its number; numbers are never reused.
        number.set(new ClaimNumber(claim.getNumber()));
        if (request.target() == ClaimStatus.SETTLED) {
            // Settling fixes the due: the figure a statement bound it to, else its amount (raised to the floor)
            // at the rate in force on the claim's date, with its late charge as of today frozen in; with no
            // figure it stays live (n/a until a rate is set).
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            BigDecimal figure = claim.isStated() && claim.getStatedDue() != null ? claim.getStatedDue()
                    : Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate()),
                    settings.floor(claim.getSheetId()));
            claim.setSettledDue(LateCharge.withCharge(claim, figure, settings.latePct(claim.getSheetId()),
                    settings.terms(claim.getSheetId()), sheets.today(claim.getSheetId())));
        }
        claim.setStatus(request.target());
    }
}
