package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriod {
    public void respond(@Val Period period, PeriodRepository repository, SettingsRepository settings,
            ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(period, repository.personTotals(period), repository.personCarried(period), repository.personOpenings(period), settings.find(period.sheetId()),
                repository.addedOrder(period.sheetId()).getOrDefault(period.name(), 0L)));
    }
}
