package net.officefloor.hq.app.pool;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The sheet's pools, then its crews as pools (each in the order they were added). */
public class LoadPools {
    public void load(@Val Sheet sheet, PoolRepository repository, SettingRepository settings, Out<Pools> pools) {
        List<Pool> list = new ArrayList<>(repository.findAll(sheet.id(), sheet.today()));
        list.addAll(repository.crewPools(sheet.id(), sheet.today()));
        pools.set(new Pools(list, settings.pot(sheet.id())));
    }
}
