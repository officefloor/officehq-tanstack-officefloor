package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools on each sheet and their members. */
@Repository
public class Pools {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    private final Settings settings;

    public Pools(JdbcTemplate jdbc, Settings settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    /** Every pool on the sheet in the order added, its members in the order added, each with their cut of the pot. */
    public List<Pool> list(String sheet) {
        Map<String, List<PoolMember>> members = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet = ? ORDER BY position",
                (rs) -> {
                    members.put(rs.getString("name"), new ArrayList<>());
                }, sheet);
        jdbc.query("SELECT m.pool, m.person_id, p.name, m.share, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ? ORDER BY m.position",
                (rs) -> {
                    List<PoolMember> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        list.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"),
                                rs.getBigDecimal("share").setScale(2).toPlainString(),
                                rs.getDate("retired_from") != null, ""));
                    }
                }, sheet);
        BigDecimal pot = settings.pot(sheet);
        List<Pool> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            boolean balanced = total.compareTo(FULL) == 0;
            pools.add(new Pool(name, balanced && pot != null ? cut(list, pot) : list, total.toPlainString(), balanced));
        });
        return pools;
    }

    /**
     * The members each with their cut of the pot: the pot times their share over 100, half to even to two decimals (a
     * retired member's 0.00); the remainder, the pot less the cuts, goes to the member with the largest share, the
     * earliest added on a tie, so the cuts total the pot.
     */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(FULL, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        List<PoolMember> out = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            out.add(new PoolMember(m.person(), m.name(), m.share(), m.retired(), cuts.get(i).toPlainString()));
        }
        return out;
    }

    /** Whether the sheet has a pool of this name. */
    public boolean has(String sheet, String name) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet = ? AND name = ?", Integer.class,
                sheet, name);
        return found != null && found > 0;
    }

    /** Whether the person is in the pool. */
    public boolean isMember(String sheet, String pool, long person) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pool_member WHERE sheet = ? AND pool = ? AND person_id = ?", Integer.class, sheet,
                pool, person);
        return found != null && found > 0;
    }

    public void add(String sheet, String name) {
        jdbc.update("INSERT INTO pool (sheet, name, position) VALUES (?, ?,"
                + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM pool p WHERE p.sheet = ?))", sheet, name, sheet);
    }

    public void addMember(String sheet, String pool, long person, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet, pool, person_id, share, position) VALUES (?, ?, ?, ?,"
                + " (SELECT COALESCE(MAX(m.position), 0) + 1 FROM pool_member m WHERE m.sheet = ?))", sheet, pool,
                person, share, sheet);
    }
}
