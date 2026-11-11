package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import org.springframework.stereotype.Component;

/**
 * Each person's cut total: their cuts in every pool they are a member of (crews too), added up, as the
 * pools stand now with {@code today} as the sheet's today. A pool with no cuts adds nothing. Reachable
 * statically, for figures (such as the rollup) worked out away from the endpoint's own dependencies.
 */
@Component
public class CutTotals {

    private static volatile CutTotals instance;

    private final PoolRepository pools;
    private final CrewRepository crews;
    private final SettingsRepository settings;

    public CutTotals(PoolRepository pools, CrewRepository crews, SettingsRepository settings) {
        this.pools = pools;
        this.crews = crews;
        this.settings = settings;
        instance = this;
    }

    /** The cut totals by person id now (a person with no cut is absent). */
    public static Map<Long, BigDecimal> of(String sheetId, LocalDate today) {
        return of(sheetId, today, null);
    }

    /** The cut totals by person id as the pools stood at the end of {@code asOf} (null: now). */
    public static Map<Long, BigDecimal> of(String sheetId, LocalDate today, LocalDate asOf) {
        return instance == null ? Map.of() : instance.find(sheetId, today, asOf);
    }

    public Map<Long, BigDecimal> find(String sheetId, LocalDate today, LocalDate asOf) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (PoolResponse pool : RespondWithPools.read(sheetId, today, asOf, pools, crews, settings)) {
            for (PoolMemberResponse m : pool.members()) {
                if (!m.memberPool().isEmpty() || m.personId().isEmpty() || m.cut().isEmpty()) continue;
                totals.merge(Long.valueOf(m.personId()), new BigDecimal(m.cut()), BigDecimal::add);
            }
        }
        return totals;
    }
}
