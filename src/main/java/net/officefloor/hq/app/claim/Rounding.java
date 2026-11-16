package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded to two decimals: a claim dated before
 * the sheet's policy date (its today while the policy date is blank) rounds half up, one dated on or after it half to
 * even. A correction rounds by the date of the claim it corrects, not its own.
 */
public record Rounding(LocalDate policy, ClaimRepository claims) {

    public static Rounding of(String sheetId, SheetRepository sheets, SettingRepository settings, ClaimRepository claims) {
        LocalDate policy = settings.policyDate(sheetId);
        return new Rounding(policy == null ? sheets.today(sheetId) : policy, claims);
    }

    /** The date that decides how the claim rounds: the corrected claim's date for a correction, else its own. */
    public static LocalDate roundsOn(Claim claim, ClaimRepository claims) {
        if (claim.corrects() != null) {
            Claim corrected = claims.findByNumber(claim.sheetId(), claim.personId(), claim.corrects());
            if (corrected != null) {
                return corrected.date();
            }
        }
        return claim.date();
    }

    /** The rounding the claim's live figures take. */
    public RoundingMode mode(Claim claim) {
        return roundsOn(claim, claims).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
