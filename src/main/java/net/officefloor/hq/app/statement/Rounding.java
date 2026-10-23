package net.officefloor.hq.app.statement;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.SettingHistory;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** How the sheet rounds a claim's live figures: by the rounding mode in force on its date (the dated rounding
 *  setting: half-up or half-even; blank: none), else by its policy date (the policy-date setting, else the sheet's
 *  today; null: none, everything rounds half to even), and the dates of its numbered claims (a correction rounds by
 *  the date of the claim it corrects). */
public record Rounding(LocalDate policy, Map<String, LocalDate> numbered, NavigableMap<LocalDate, String> modes) {

    public Rounding(LocalDate policy, Map<String, LocalDate> numbered) {
        this(policy, numbered, new TreeMap<>());
    }

    /** The sheet's rounding as it is now. */
    public static Rounding of(String sheetId, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims) {
        return of(sheetId, settings, sheets, claims, settings.history(sheetId));
    }

    /** The sheet's rounding with the rounding modes of the given dated settings. */
    public static Rounding of(String sheetId, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims, SettingHistory history) {
        LocalDate policy = settings.policyDate(sheetId);
        Map<String, LocalDate> numbered = new HashMap<>();
        for (Claim c : claims.list(sheetId)) {
            if (c.number() != null) {
                numbered.put(c.number(), c.date());
            }
        }
        return new Rounding(policy != null ? policy : sheets.today(sheetId), numbered,
                history.of(SettingHistory.ROUNDING));
    }

    /** Every figure rounded half to even. */
    public static Rounding none() {
        return new Rounding(null, Map.of());
    }

    /** The same rounding with the rounding modes of the given dated settings. */
    public Rounding withModes(SettingHistory history) {
        return new Rounding(policy, numbered, history.of(SettingHistory.ROUNDING));
    }

    /** How a claim's live figures round: by the rounding mode in force on its date (a correction: the date of the
     *  claim it corrects) when there is one; else half up when dated before the policy date, else half to even. */
    public RoundingMode of(Claim claim) {
        LocalDate date = claim.corrects() != null ? numbered.getOrDefault(claim.corrects(), claim.date())
                : claim.date();
        var mode = modes.floorEntry(date);
        if (mode != null && mode.getValue() != null) {
            return SettingHistory.HALF_UP.equals(mode.getValue()) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
        }
        return policy != null && date.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
