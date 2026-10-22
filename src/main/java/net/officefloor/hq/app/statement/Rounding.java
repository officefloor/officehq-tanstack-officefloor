package net.officefloor.hq.app.statement;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** How the sheet rounds a claim's live figures: by its policy date (the policy-date setting, else the sheet's today;
 *  null: none, everything rounds half to even), and the dates of its numbered claims (a correction rounds by the
 *  date of the claim it corrects). */
public record Rounding(LocalDate policy, Map<String, LocalDate> numbered) {

    /** The sheet's rounding as it is now. */
    public static Rounding of(String sheetId, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims) {
        LocalDate policy = settings.policyDate(sheetId);
        Map<String, LocalDate> numbered = new HashMap<>();
        for (Claim c : claims.list(sheetId)) {
            if (c.number() != null) {
                numbered.put(c.number(), c.date());
            }
        }
        return new Rounding(policy != null ? policy : sheets.today(sheetId), numbered);
    }

    /** Every figure rounded half to even. */
    public static Rounding none() {
        return new Rounding(null, Map.of());
    }

    /** How a claim's live figures round: half up when it (a correction: the claim it corrects) is dated before the
     *  policy date, else half to even. */
    public RoundingMode of(Claim claim) {
        LocalDate date = claim.corrects() != null ? numbered.getOrDefault(claim.corrects(), claim.date())
                : claim.date();
        return policy != null && date.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
