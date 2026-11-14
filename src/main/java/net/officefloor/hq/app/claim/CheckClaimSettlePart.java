package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a part-settle the settle button would refuse, judged on the original claim before anything is
 * written: one not OPEN or deferred, a role not allowed to settle, the user who opened it or last edited it (four
 * eyes), or one with no proofs (of each of the sheet's proof kinds) whose due is above the proof threshold.
 */
public class CheckClaimSettlePart {
    public void check(@RequestParam("user") String user, @Val Claim claim, SheetRepository sheets,
            PersonRepository people, RateRepository rates, SettingRepository settings, ClaimRepository claims)
            throws InvalidClaimException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be part-settled: only an OPEN one.");
        }
        if (!settings.find(claim.sheetId()).may(user, "settle")) {
            throw new InvalidClaimException("The user's role is not allowed to settle a claim.");
        }
        if (claim.deferred(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be settled yet.");
        }
        if (user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot settle it.");
        }
        if (user.equals(claims.editedBy(claim.sheetId(), claim.id()))) {
            throw new InvalidClaimException("The user who last edited a claim cannot settle it.");
        }
        BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
        if (!ApplyClaimAction.hasProofs(claim, settings, claims) && settings.dated(claim.sheetId())
                .needsProof(ApplyClaimAction.proofGrade(claim, people), claim.date(), due)) {
            throw new InvalidClaimException("The claim's due is above the proof threshold: it needs a proof (of each of the sheet's proof kinds) to be settled.");
        }
    }
}
