package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** How a sheet rounds a claim's live figures (its rate step and late charge): a claim dated before the
 *  sheet's policy date (the one set, else the sheet's today) rounds half up, one dated on or after it
 *  half to even; but a claim whose date has a rounding mode in force (a dated setting) rounds by that mode.
 *  A correction rounds by the date of the claim it corrects, not its own. */
public final class Rounding {

    private final LocalDate policyDate;

    /** The rounding mode in force on each date, or null when none is (then the policy date decides). */
    private final Function<LocalDate, RoundingMode> modes;

    /** The date of each opened claim on the sheet, by its person and number. */
    private final Map<String, LocalDate> dates = new HashMap<>();

    public Rounding(LocalDate policyDate, List<Claim> claims) {
        this(policyDate, date -> null, claims);
    }

    public Rounding(LocalDate policyDate, Function<LocalDate, RoundingMode> modes, List<Claim> claims) {
        this.policyDate = policyDate;
        this.modes = modes;
        for (Claim claim : claims) {
            if (claim.getNumber() != null) {
                dates.put(key(claim.getPersonId(), claim.getNumber()), claim.getDate());
            }
        }
    }

    /** The sheet's rounding, knowing every claim on it (so a correction finds the claim it corrects). */
    public static Rounding of(String sheetId, SettingStore settings, SheetStore sheets, ClaimStore claims) {
        LocalDate policy = settings.policyDate(sheetId);
        return new Rounding(policy != null ? policy : sheets.today(sheetId), settings.roundings(sheetId),
                claims.list(sheetId));
    }

    /** The date the claim rounds by: its own, or for a correction the date of the claim it corrects. */
    public LocalDate date(Claim claim) {
        LocalDate date = claim.getDate();
        if (claim.getCorrects() != null) {
            date = dates.getOrDefault(key(claim.getPersonId(), claim.getCorrects()), date);
        }
        return date;
    }

    /** The rounding of the claim's live figures. */
    public RoundingMode mode(Claim claim) {
        RoundingMode mode = modes.apply(date(claim));
        if (mode != null) {
            return mode;
        }
        return policyDate != null && date(claim).isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    private static String key(long personId, String number) {
        return personId + "/" + number;
    }
}
