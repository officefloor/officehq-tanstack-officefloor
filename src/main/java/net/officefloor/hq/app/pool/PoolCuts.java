package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.crew.Crews;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The store of the pool members' cuts fixed for each (soft-)closed period, kept per sheet. */
@Repository
public class PoolCuts {

    private final JdbcTemplate jdbc;
    private final Pools pools;
    private final People people;
    private final Settings settings;
    private final Sheets sheets;
    private final Crews crews;

    public PoolCuts(JdbcTemplate jdbc, Pools pools, People people, Settings settings, Sheets sheets, Crews crews) {
        this.jdbc = jdbc;
        this.pools = pools;
        this.people = people;
        this.settings = settings;
        this.sheets = sheets;
        this.crews = crews;
    }

    /** Fixes, afresh, every pool member's cut for the period as it stands now (the sheet's pools on its today). */
    public void fix(String sheetId, String period) {
        drop(sheetId, period);
        for (PoolResponse pool : RespondWithPools.figure(sheetId, pools, people, settings, sheets, crews, Map.of(),
                List.of())) {
            for (PoolResponse.Member m : pool.members()) {
                jdbc.update("MERGE INTO period_pool_cut (sheet_id, period_name, pool_name, member, cut) KEY (sheet_id, period_name, pool_name, member) VALUES (?, ?, ?, ?, ?)",
                        sheetId, period, pool.name(), RespondWithPools.member(m), m.cut());
            }
        }
    }

    /**
     * Each person's cut total, by person id: their live cut in every pool they are a member of, added up (a pool that
     * shows no cut adds nothing); a person in no pool is left out.
     */
    public Map<Long, BigDecimal> totals(String sheetId) {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (PoolResponse pool : RespondWithPools.figure(sheetId, pools, people, settings, sheets, crews, Map.of(),
                List.of())) {
            for (PoolResponse.Member m : pool.members()) {
                if (m.pool() == null && !m.cut().isEmpty()) {
                    out.merge(m.personId(), new BigDecimal(m.cut()), BigDecimal::add);
                }
            }
        }
        return out;
    }

    /** Drops the period's fixed cuts, so they are live again. */
    public void drop(String sheetId, String period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, period);
    }

    /** The sheet's fixed cuts: by period name, then by pool name and member (see {@link RespondWithPools#key}). */
    public Map<String, Map<String, String>> fixed(String sheetId) {
        Map<String, Map<String, String>> out = new HashMap<>();
        jdbc.query("SELECT period_name, pool_name, member, cut FROM " + AsOf.table("period_pool_cut") + " WHERE sheet_id = ?",
                (RowCallbackHandler) rs -> out.computeIfAbsent(rs.getString("period_name"), k -> new HashMap<>())
                        .put(rs.getString("pool_name") + "\n" + rs.getString("member"), rs.getString("cut")),
                sheetId);
        return out;
    }
}
