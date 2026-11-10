package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Logs the claim's status as it was saved: who changed it, the sheet's today and its due as the
 * screen computes it at this moment (late charge included). Runs after every rule has passed, so a
 * refused change logs nothing.
 */
public class LogClaimStatus {
    public void log(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            SettingsRepository settings, ClaimLogRepository log, HttpServletRequest request) {
        log.record(claim, Audit.user(request), sheet.today(), due(sheet, claim, people, rates, settings));
    }

    /** The claim's due now, as the screen shows it; null for n/a. */
    static BigDecimal due(Sheet sheet, Claim claim, PersonRepository people, RateRepository rates,
            SettingsRepository settings) {
        return ClaimFigure.withLate(claim, rates.findAll(sheet.id()),
                people.find(sheet.id(), claim.getPersonId()).gradeOn(claim.getDate()), settings.find(sheet.id()),
                sheet.today());
    }
}
