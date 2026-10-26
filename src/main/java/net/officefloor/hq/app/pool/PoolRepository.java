package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.access.Setting;
import net.officefloor.hq.app.access.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools of each sheet, in the order added, each name at most once on its sheet, and their members. */
@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The id of the sheet's pool of that name; null when there is none. */
    public Long findId(String sheetId, String name) {
        return jdbc.query("SELECT id FROM pool WHERE sheet_id = ? AND name = ?", (rs, n) -> rs.getLong(1), sheetId, name)
                .stream().findFirst().orElse(null);
    }

    public void insert(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    /** Adds the person to the pool; false when they are in it already. */
    public boolean addMember(String sheetId, long poolId, long personId, BigDecimal share) {
        return jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share) SELECT ?, ?, ?, ? WHERE NOT EXISTS"
                + " (SELECT 1 FROM pool_member WHERE pool_id = ? AND person_id = ?)",
                sheetId, poolId, personId, share, poolId, personId) > 0;
    }

    /**
     * The sheet's pools, in the order added, each with its members in the order added, the total of their shares (a
     * member with a retirement date set, whatever the date, counts as 0.00) and whether it is exactly 100.00. Each
     * member of a balanced pool on a sheet with a pot has a cut: the pot times their share over 100, half to even to
     * two decimals (0.00 when retired), and the remainder to the member with the largest share (the earliest added on
     * a tie), so the cuts total the pot.
     */
    public List<PoolResponse> list(String sheetId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            names.put(rs.getLong("id"), rs.getString("name"));
            members.put(rs.getLong("id"), new ArrayList<>());
        }, sheetId);
        jdbc.query("SELECT m.pool_id, m.person_id, m.share, p.name, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<PoolResponse.Member> of = members.get(rs.getLong("pool_id"));
            if (of != null) {
                of.add(new PoolResponse.Member(rs.getLong("person_id"), rs.getString("name"),
                        rs.getBigDecimal("share").setScale(2).toPlainString(), rs.getDate("retired_from") != null, null));
            }
        }, sheetId);
        String potValue = new SettingRepository(jdbc).find(sheetId, Setting.POT);
        BigDecimal pot = potValue == null ? null : new BigDecimal(potValue);
        List<PoolResponse> pools = new ArrayList<>();
        names.forEach((id, name) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : members.get(id)) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<PoolResponse.Member> of = balanced && pot != null ? cut(members.get(id), pot) : members.get(id);
            pools.add(new PoolResponse(name, of, total.toPlainString(), balanced));
        });
        return pools;
    }

    /** The members with their cuts of the pot; the remainder goes to the largest share, the earliest added on a tie. */
    private static List<PoolResponse.Member> cut(List<PoolResponse.Member> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(new BigDecimal(m.share())).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0
                    || new BigDecimal(m.share()).compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        List<PoolResponse.Member> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            cut.add(members.get(i).withCut(cuts.get(i).toPlainString()));
        }
        return cut;
    }
}
