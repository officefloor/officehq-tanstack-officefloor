package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Component;

/**
 * The sheet's pools, then its crews as pools, as they stand on its today; and each pool member's cut for a period,
 * fixed as it stands now when the period is closed or soft-closed (by hand or automatically) and live again when it
 * reopens.
 */
@Component
public class PoolCuts {

    private final PoolRepository repository;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public PoolCuts(PoolRepository repository, SettingRepository settings, SheetRepository sheets) {
        this.repository = repository;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The sheet's pools, then its crews as pools, with the sheet's pot, as they stand on {@code today}. */
    public Pools live(String sheetId, LocalDate today) {
        List<Pool> list = new ArrayList<>(repository.findAll(sheetId, today));
        list.addAll(repository.crewPools(sheetId, today));
        return new Pools(list, settings.pot(sheetId));
    }

    /** Fix every pool member's cut for the period as it stands now (the cell it shows now). */
    public void fix(String sheetId, String period) {
        Pools pools = live(sheetId, sheets.today(sheetId));
        Map<String, String> fixed = new LinkedHashMap<>();
        for (Pool pool : pools.list()) {
            List<BigDecimal> cuts = PoolResponse.cuts(pool, pools.potOf(pool));
            for (int i = 0; i < pool.members().size(); i++) {
                PoolMember m = pool.members().get(i);
                fixed.put(PoolRepository.cutKey(pool.id(), m.personId(), m.poolId()),
                        cuts == null || cuts.get(i) == null ? "" : cuts.get(i).toPlainString());
            }
        }
        repository.saveFixedCuts(sheetId, period, fixed);
    }

    /** Make the period's cuts live again. */
    public void unfix(String sheetId, String period) {
        repository.clearFixedCuts(sheetId, period);
    }
}
