package net.officefloor.hq.app.pool;

import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The pool members' cuts fixed for closed periods: closing or soft-closing a period fixes every pool
 * member's cut as it stood at that moment, and reopening it clears them, making them live again.
 */
@Repository
public class FrozenCuts {

    private final JdbcTemplate jdbc;
    private final PoolRepository pools;
    private final CrewRepository crews;
    private final SettingsRepository settings;

    public FrozenCuts(JdbcTemplate jdbc, PoolRepository pools, CrewRepository crews, SettingsRepository settings) {
        this.jdbc = jdbc;
        this.pools = pools;
        this.crews = crews;
        this.settings = settings;
    }

    /** Fix each pool member's cut for the closed period, as it stands now with the sheet's today. */
    public void freeze(Sheet sheet, Period period) {
        thaw(period);
        for (PoolResponse pool : RespondWithPools.read(sheet.id(), sheet.today(), null, pools, crews, settings)) {
            for (PoolMemberResponse m : pool.members()) {
                jdbc.update("MERGE INTO period_pool_cut (sheet_id, period_name, pool, member_key, cut) KEY (sheet_id, period_name, pool, member_key)"
                        + " VALUES (?, ?, ?, ?, ?)", sheet.id(), period.name(), pool.name(), RespondWithPools.key(m), m.cut());
            }
        }
    }

    /** Clear the members' fixed cuts for the reopened period. */
    public void thaw(Period period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", period.sheetId(), period.name());
    }

    /** The fixed cuts for the sheet's closed periods: by period name, then pool name, then member key. */
    public Map<String, Map<String, Map<String, String>>> find(String sheetId) {
        Map<String, Map<String, Map<String, String>>> found = new HashMap<>();
        jdbc.query("SELECT period_name, pool, member_key, cut FROM period_pool_cut WHERE sheet_id = ?", rs -> {
            found.computeIfAbsent(rs.getString("period_name"), k -> new HashMap<>())
                    .computeIfAbsent(rs.getString("pool"), k -> new HashMap<>())
                    .put(rs.getString("member_key"), rs.getString("cut"));
        }, sheetId);
        return found;
    }
}
