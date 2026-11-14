package net.officefloor.hq.app.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The sheet's pools, then its crews as pools (each in the order they were added), and their cuts for each period:
 * fixed as they stood when it was closed or soft-closed, whatever its to-date; else, for an open period whose to-date
 * is on or before the sheet's today, as they stood at its end.
 */
public class LoadPools {
    public void load(@Val Sheet sheet, PoolRepository repository, PoolCuts poolCuts, SettingRepository settings,
            PeriodRepository periods, Out<Pools> pools, Out<PoolHistory> history) {
        pools.set(poolCuts.live(sheet.id(), sheet.today()));
        Map<String, Map<String, String>> fixed = repository.fixedCuts(sheet.id());
        List<PoolHistory.Ended> ended = new ArrayList<>();
        for (Period period : periods.findAll(sheet.id())) {
            if (period.fixed() && fixed.containsKey(period.name())) {
                ended.add(new PoolHistory.Ended(period.name(), null, fixed.get(period.name())));
                continue;
            }
            if (period.to().isAfter(sheet.today())) {
                continue;
            }
            List<Pool> then = new ArrayList<>(repository.findAll(sheet.id(), period.to(), true));
            then.addAll(repository.crewPools(sheet.id(), period.to(), true));
            ended.add(new PoolHistory.Ended(period.name(),
                    new Pools(then, settings.potOn(sheet.id(), period.to())), null));
        }
        history.set(new PoolHistory(ended));
    }
}
