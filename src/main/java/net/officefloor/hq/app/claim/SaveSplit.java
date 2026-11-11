package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Saves the split: the original with the rest of its amount, and the new claim, unnumbered until it
 * is settled, its status logged as it was opened.
 */
public class SaveSplit {
    public void save(@Val Sheet sheet, @Val Claim claim, @Val Split split, ClaimRepository repository,
            SettingsRepository settings, PeriodRepository periods, PersonRepository people, RateRepository rates,
            ClaimLogRepository log, HttpServletRequest request) {
        repository.save(claim);
        Claim made = split.made();
        repository.save(made);
        log.record(made, Audit.user(request), sheet.today(), LogClaimStatus.due(sheet, made, people, rates, settings));
    }
}
