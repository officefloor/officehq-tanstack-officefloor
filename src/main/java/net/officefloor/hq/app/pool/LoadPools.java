package net.officefloor.hq.app.pool;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The sheet's pools, then its crews as pools (each in the order they were added), and as they stood at the end of each
 * period whose to-date is on or before the sheet's today.
 */
public class LoadPools {
    public void load(@Val Sheet sheet, PoolRepository repository, SettingRepository settings,
            PeriodRepository periods, Out<Pools> pools, Out<PoolHistory> history) {
        List<Pool> list = new ArrayList<>(repository.findAll(sheet.id(), sheet.today()));
        list.addAll(repository.crewPools(sheet.id(), sheet.today()));
        pools.set(new Pools(list, settings.pot(sheet.id())));
        List<PoolHistory.Ended> ended = new ArrayList<>();
        for (Period period : periods.findAll(sheet.id())) {
            if (period.to().isAfter(sheet.today())) {
                continue;
            }
            List<Pool> then = new ArrayList<>(repository.findAll(sheet.id(), period.to(), true));
            then.addAll(repository.crewPools(sheet.id(), period.to(), true));
            ended.add(new PoolHistory.Ended(period.name(), new Pools(then, settings.potOn(sheet.id(), period.to()))));
        }
        history.set(new PoolHistory(ended));
    }
}
