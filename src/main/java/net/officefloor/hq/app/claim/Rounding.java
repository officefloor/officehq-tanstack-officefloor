package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** How a sheet rounds a claim's live figures (its rate step and late charge): a claim dated before the
 *  sheet's policy date (the one set, else the sheet's today) rounds half up, one dated on or after it
 *  half to even. A correction rounds by the date of the claim it corrects, not its own. */
public final class Rounding {

    private final LocalDate policyDate;

    /** The date of each opened claim on the sheet, by its person and number. */
    private final Map<String, LocalDate> dates = new HashMap<>();

    public Rounding(LocalDate policyDate, List<Claim> claims) {
        this.policyDate = policyDate;
        for (Claim claim : claims) {
            if (claim.getNumber() != null) {
                dates.put(key(claim.getPersonId(), claim.getNumber()), claim.getDate());
            }
        }
    }

    /** The sheet's rounding, knowing every claim on it (so a correction finds the claim it corrects). */
    public static Rounding of(String sheetId, SettingStore settings, SheetStore sheets, ClaimStore claims) {
        LocalDate policy = settings.policyDate(sheetId);
        return new Rounding(policy != null ? policy : sheets.today(sheetId), claims.list(sheetId));
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
        return policyDate != null && date(claim).isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    private static String key(long personId, long number) {
        return personId + "/" + number;
    }
}
