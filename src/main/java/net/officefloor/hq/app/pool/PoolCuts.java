package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.crew.CrewStore;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Each pool member's cut fixed for a period when it is closed or soft-closed: as the cuts stand at that moment
 *  (the sheet's today as today), kept until the period is reopened. */
@Service
public class PoolCuts {

    /** A member's cut fixed in a period: the pool, the person or the member pool, and the cut (null for none). */
    public record Fixed(long poolId, Long personId, Long memberPoolId, BigDecimal cut) {
    }

    private final JdbcTemplate jdbc;
    private final PoolStore store;
    private final SettingStore settings;
    private final PersonStore people;
    private final SheetStore sheets;
    private final CrewStore crews;

    public PoolCuts(JdbcTemplate jdbc, PoolStore store, SettingStore settings, PersonStore people, SheetStore sheets,
            CrewStore crews) {
        this.jdbc = jdbc;
        this.store = store;
        this.settings = settings;
        this.people = people;
        this.sheets = sheets;
        this.crews = crews;
    }

    /** Fixes every pool member's cut in the period as it stands now, in place of any fixed before. */
    public void fix(String sheet, String period) {
        drop(sheet, period);
        RespondWithPools.Cuts now = RespondWithPools.cuts(sheet, null, sheets.today(sheet), store, settings, people,
                crews);
        for (PoolStore.Pool p : now.pools()) {
            List<PoolStore.Member> ms = now.members().getOrDefault(p.id(), List.of());
            List<BigDecimal> cs = now.cuts().get(p.id());
            for (int i = 0; i < ms.size(); i++) {
                PoolStore.Member m = ms.get(i);
                jdbc.update("INSERT INTO period_pool_cut (sheet_id, period_name, pool_id, person_id, member_pool_id, cut)"
                        + " VALUES (?, ?, ?, ?, ?, ?)", sheet, period, p.id(), m.personId(), m.memberPoolId(),
                        cs == null ? null : cs.get(i));
            }
        }
    }

    /** Drops the cuts fixed in the period (it is live again). */
    public void drop(String sheet, String period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheet, period);
    }

    /** The cuts fixed in the period. */
    public List<Fixed> fixed(String sheet, String period) {
        List<Fixed> found = new ArrayList<>();
        jdbc.query("SELECT pool_id, person_id, member_pool_id, cut FROM period_pool_cut"
                + " WHERE sheet_id = ? AND period_name = ? ORDER BY id", rs -> {
                    found.add(new Fixed(rs.getLong("pool_id"),
                            rs.getObject("person_id") == null ? null : rs.getLong("person_id"),
                            rs.getObject("member_pool_id") == null ? null : rs.getLong("member_pool_id"),
                            rs.getBigDecimal("cut")));
                }, sheet, period);
        return found;
    }

    /** The member's cut in the pool among those fixed, null when it had none. */
    static String cutOf(List<Fixed> fixed, long poolId, PoolStore.Member m) {
        return fixed.stream()
                .filter(f -> f.poolId() == poolId && Objects.equals(f.personId(), m.personId())
                        && Objects.equals(f.memberPoolId(), m.memberPoolId()))
                .findFirst().map(f -> f.cut() == null ? null : f.cut().toPlainString()).orElse(null);
    }
}
