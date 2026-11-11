package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Gives a claim just settled or approved its number (the next in its sequence); a claim sent to PENDING
 * has none yet, and a claim already numbered keeps its number.
 */
public class NumberSettledClaim {
    public void number(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository, SettingsRepository settings,
            PeriodRepository periods) {
        ClaimNumbers.giveOnSettle(sheet, claim, settings.find(sheet.id()), periods, repository);
    }
}
