package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

    /**
     * Every pool on the sheet in the order added, its members (people and pools) in the order added, each with their
     * cut of the pool's pot: a pool that is a member of none has the sheet's pot; a pool that is a member of another
     * has its cut there as its pot (none when that pool is unbalanced or has none).
     */
    public List<Pool> list(String sheet) {
        Map<String, List<Ordered>> members = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet = ? ORDER BY position",
                (rs) -> {
                    members.put(rs.getString("name"), new ArrayList<>());
                }, sheet);
        jdbc.query("SELECT m.pool, m.person_id, p.name, m.share, p.retired_from, m.position FROM pool_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ?",
                (rs) -> {
                    List<Ordered> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        list.add(new Ordered(rs.getInt("position"), new PoolMember(rs.getLong("person_id"), null,
                                rs.getString("name"), rs.getBigDecimal("share").setScale(2).toPlainString(),
                                rs.getDate("retired_from") != null, "")));
                    }
                }, sheet);
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT pool, member, share, position FROM pool_sub WHERE sheet = ?",
                (rs) -> {
                    List<Ordered> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        list.add(new Ordered(rs.getInt("position"), new PoolMember(0, rs.getString("member"),
                                rs.getString("member"), rs.getBigDecimal("share").setScale(2).toPlainString(), false,
                                "")));
                        parents.put(rs.getString("member"), rs.getString("pool"));
                    }
                }, sheet);
        Map<String, Pool> pools = new LinkedHashMap<>();
        members.forEach((name, ordered) -> {
            ordered.sort(Comparator.comparingInt(Ordered::position));
            List<PoolMember> list = ordered.stream().map(Ordered::member).toList();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            pools.put(name, new Pool(name, list, total.toPlainString(), total.compareTo(FULL) == 0));
        });
        // Top down: a pool's pot is the sheet's when it is a member of none, else its cut in the pool above.
        BigDecimal pot = settings.pot(sheet);
        Map<String, Pool> cut = new HashMap<>();
        for (String name : pools.keySet()) {
            cutDown(name, pools, parents, pot, cut);
        }
        return pools.keySet().stream().map(cut::get).toList();
    }

    private record Ordered(int position, PoolMember member) {
    }

    /** The pool with its members' cuts, its pool above cut first. */
    private static Pool cutDown(String name, Map<String, Pool> pools, Map<String, String> parents, BigDecimal sheetPot,
            Map<String, Pool> done) {
        Pool known = done.get(name);
        if (known != null) {
            return known;
        }
        Pool pool = pools.get(name);
        BigDecimal pot = sheetPot;
        String parent = parents.get(name);
        if (parent != null && pools.containsKey(parent)) {
            pot = null;
            for (PoolMember m : cutDown(parent, pools, parents, sheetPot, done).members()) {
                if (name.equals(m.memberPool()) && !m.cut().isEmpty()) {
                    pot = new BigDecimal(m.cut());
                }
            }
        }
        Pool out = pool.balanced() && pot != null
                ? new Pool(name, cut(pool.members(), pot), pool.totalShare(), true)
                : pool;
        done.put(name, out);
        return out;
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
            out.add(new PoolMember(m.person(), m.memberPool(), m.name(), m.share(), m.retired(), cuts.get(i).toPlainString()));
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

    /** The pool this pool is a member of, or null. */
    public String parentOf(String sheet, String pool) {
        List<String> found = jdbc.queryForList("SELECT pool FROM pool_sub WHERE sheet = ? AND member = ?",
                String.class, sheet, pool);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The pools that are members of this pool. */
    public List<String> subPools(String sheet, String pool) {
        return jdbc.queryForList("SELECT member FROM pool_sub WHERE sheet = ? AND pool = ?", String.class, sheet,
                pool);
    }

    public void add(String sheet, String name) {
        jdbc.update("INSERT INTO pool (sheet, name, position) VALUES (?, ?,"
                + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM pool p WHERE p.sheet = ?))", sheet, name, sheet);
    }

    public void addMember(String sheet, String pool, long person, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet, pool, person_id, share, position) VALUES (?, ?, ?, ?, "
                + NEXT_MEMBER + ")", sheet, pool, person, share, sheet, sheet);
    }

    public void addSubPool(String sheet, String pool, String member, BigDecimal share) {
        jdbc.update("INSERT INTO pool_sub (sheet, pool, member, share, position) VALUES (?, ?, ?, ?, " + NEXT_MEMBER
                + ")", sheet, pool, member, share, sheet, sheet);
    }

    /** The next position among all pools' members, people and pools. */
    private static final String NEXT_MEMBER = "(SELECT COALESCE(MAX(x.position), 0) + 1 FROM (SELECT position FROM"
            + " pool_member WHERE sheet = ? UNION ALL SELECT position FROM pool_sub WHERE sheet = ?) x)";
}
