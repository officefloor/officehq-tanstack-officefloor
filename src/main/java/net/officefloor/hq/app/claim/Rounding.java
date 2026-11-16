package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded to two decimals: a claim dated before
 * the sheet's policy date (its today while the policy date is blank) rounds half up, one dated on or after it half to
 * even; unless a rounding mode (the dated setting rounding, half-up or half-even) is in force on its date, when it rounds
 * by that mode. A correction rounds by the date of the claim it corrects, not its own.
 */
public record Rounding(LocalDate policy, ClaimRepository claims, Function<LocalDate, String> modeOn) {

    public static Rounding of(String sheetId, SheetRepository sheets, SettingRepository settings, ClaimRepository claims) {
        LocalDate policy = settings.policyDate(sheetId);
        return new Rounding(policy == null ? sheets.today(sheetId) : policy, claims,
                (date) -> settings.value(sheetId, SettingRepository.ROUNDING, date));
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
        LocalDate on = roundsOn(claim, claims);
        String mode = modeOn.apply(on);
        if (SettingRepository.HALF_UP.equals(mode)) {
            return RoundingMode.HALF_UP;
        }
        if (SettingRepository.HALF_EVEN.equals(mode)) {
            return RoundingMode.HALF_EVEN;
        }
        return on.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
