package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored pools and their members, kept per sheet. */
@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    private final SettingRepository settings;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    /** The sheet's pools in the order added, each with its members in the order added. A retired member's share (one
     *  with a retirement date, whatever the date) counts as 0.00 toward the total; balanced is a total of exactly 100.00.
     *  Each member of a balanced pool has their cut of the pool's pot (see cuts): a pool that is a member of none has the
     *  sheet's pot, a member pool its cut in the pool above (none when that pool has no cuts); with no pot, no one has. */
    public List<Pool> list(String sheetId) {
        BigDecimal pot = settings.pot(sheetId);
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                }, sheetId);
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, p.name, p.retired_from, q.name AS pool_name"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ? ORDER BY m.id",
                rs -> {
                    long poolId = rs.getLong("pool_id");
                    List<PoolMember> in = members.computeIfAbsent(poolId, k -> new ArrayList<>());
                    BigDecimal share = rs.getBigDecimal("share").setScale(2);
                    long memberPool = rs.getLong("member_pool_id");
                    if (rs.wasNull()) {
                        in.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"), null, share,
                                rs.getDate("retired_from") != null, null));
                    } else {
                        String name = rs.getString("pool_name");
                        in.add(new PoolMember(null, name, name, share, false, null));
                        parents.put(memberPool, poolId);
                    }
                }, sheetId);
        Map<Long, Pool> pools = new LinkedHashMap<>();
        for (Long id : names.keySet()) {
            share(id, names, members, parents, pot, pools);
        }
        return new ArrayList<>(pools.values());
    }

    /** Builds the pool (and, first, the pool above it), its members cut from its pot when it is balanced. */
    private Pool share(Long id, Map<Long, String> names, Map<Long, List<PoolMember>> members, Map<Long, Long> parents,
            BigDecimal sheetPot, Map<Long, Pool> pools) {
        Pool done = pools.get(id);
        if (done != null) {
            return done;
        }
        BigDecimal pot = sheetPot;
        Long parent = parents.get(id);
        if (parent != null) {
            pot = null;
            Pool above = share(parent, names, members, parents, sheetPot, pools);
            for (PoolMember m : above.members()) {
                if (names.get(id).equals(m.pool()) && m.cut() != null) {
                    pot = new BigDecimal(m.cut());
                }
            }
        }
        List<PoolMember> in = members.getOrDefault(id, List.of());
        BigDecimal total = new BigDecimal("0.00");
        for (PoolMember m : in) {
            if (!m.retired()) {
                total = total.add(m.share());
            }
        }
        boolean balanced = total.compareTo(HUNDRED) == 0;
        Pool pool = new Pool(names.get(id), balanced && pot != null ? cuts(in, pot) : in, total, balanced);
        pools.put(id, pool);
        return pool;
    }

    /** The members with their cuts of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (a retired member's 0.00); the pot less the cuts added up goes to the member with the largest share (not
     *  retired), the earliest added on a tie, so the cuts total exactly the pot. */
    static List<PoolMember> cuts(List<PoolMember> in, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = new BigDecimal("0.00");
        int largest = -1;
        for (int i = 0; i < in.size(); i++) {
            PoolMember m = in.get(i);
            BigDecimal cut = m.retired() ? new BigDecimal("0.00")
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(in.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        List<PoolMember> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            out.add(in.get(i).withCut(cuts.get(i)));
        }
        return out;
    }

    /** Adds the pool; false when the name is already taken on the sheet (the unique key refuses a second). */
    public boolean add(String sheetId, String name) {
        try {
            jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The id of the sheet's pool by that name, or null when there is none. */
    public Long idOf(String sheetId, String name) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM pool WHERE sheet_id = ? AND name = ?", Long.class, sheetId,
                name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** The id of the pool the pool is a member of, or null when it is a member of none. */
    public Long parentOf(long poolId) {
        List<Long> ids = jdbc.queryForList("SELECT pool_id FROM pool_member WHERE member_pool_id = ?", Long.class,
                poolId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** How many pools deep the chain from the pool down is: 1 for a pool with no member pools. */
    public int height(long poolId) {
        int deepest = 0;
        for (Long member : jdbc.queryForList(
                "SELECT member_pool_id FROM pool_member WHERE pool_id = ? AND member_pool_id IS NOT NULL", Long.class,
                poolId)) {
            deepest = Math.max(deepest, height(member));
        }
        return deepest + 1;
    }

    /** Adds the member pool to the pool (it is checked to be a member of none). */
    public void addPoolMember(long poolId, long memberPoolId, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (pool_id, member_pool_id, share) VALUES (?, ?, ?)", poolId, memberPoolId,
                share);
    }

    /** The name of the person on the sheet, or null when there is no such person there. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Adds the person to the pool; false when they are already in it. */
    public boolean addMember(long poolId, long personId, BigDecimal share) {
        try {
            jdbc.update("INSERT INTO pool_member (pool_id, person_id, share) VALUES (?, ?, ?)", poolId, personId, share);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
