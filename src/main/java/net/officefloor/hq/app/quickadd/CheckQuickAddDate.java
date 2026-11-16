package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.web.bind.annotation.PathVariable;

/** The claim is dated the sheet's entry date, else its today, which may not be inside a closed period. Checked before a new person is
 *  added, so a refused claim leaves no person and no audit record behind. */
public class CheckQuickAddDate {
    public void check(@PathVariable("sheetId") String sheetId, SettingRepository settings, PeriodRepository periods)
            throws InvalidClaimException {
        String sheet = sheetId.trim();
        if (periods.closedOn(sheet, settings.claimDate(sheet))) {
            throw new InvalidClaimException("The claim's date is inside a closed period");
        }
    }
}
