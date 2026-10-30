package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pool store: each sheet's pools, in the order added, and their members. */
@Repository
public class Pools {

    /** The shares of a balanced pool total exactly this. */
    public static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The deepest a chain of pools nests: a pool, a pool in it, and a pool in that. */
    public static final int MAX_DEPTH = 3;

    /** The sheet's pools, in the order added, each with its members (people and pools) in the order added. A retired
     *  member (a retirement date set, whatever the date) counts 0.00 toward the total. A pool that is a member of none
     *  has the sheet's pot; a pool that is a member of another has its cut there as its pot. While a pool has a pot,
     *  each member of it, if balanced, has a cut of it (see {@link #cuts}); otherwise no cut (""). */
    public List<PoolResponse> list(String sheetId) {
        Map<String, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY seq", String.class, sheetId)) {
            members.put(name, new ArrayList<>());
        }
        jdbc.query("SELECT m.pool_name, m.person_id, p.name, m.share, p.retired_from, m.member_pool FROM pool_member m"
                + " LEFT JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " WHERE m.sheet_id = ? ORDER BY m.seq", rs -> {
                    List<PoolResponse.Member> list = members.get(rs.getString(1));
                    String sub = rs.getString(6);
                    String share = rs.getBigDecimal(4).setScale(2).toPlainString();
                    if (list == null) {
                        return;
                    }
                    if (sub != null) {
                        list.add(new PoolResponse.Member(null, sub, sub, share, false, ""));
                    } else if (rs.getString(3) != null) {
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), null, rs.getString(3),
                                share, rs.getDate(5) != null, ""));
                    }
                }, sheetId);
        List<BigDecimal> pots = jdbc.queryForList("SELECT pot FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        BigDecimal pot = pots.isEmpty() || pots.get(0) == null ? null : pots.get(0).setScale(2);
        Map<String, String> totals = new HashMap<>();
        Set<String> nested = new HashSet<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
                if (m.memberPool() != null) {
                    nested.add(m.memberPool());
                }
            }
            totals.put(name, total.toPlainString());
        });
        Map<String, List<PoolResponse.Member>> shown = new HashMap<>(members);
        for (String name : members.keySet()) {
            if (!nested.contains(name)) {
                share(name, pot, members, totals, shown, 0);
            }
        }
        List<PoolResponse> pools = new ArrayList<>();
        members.keySet().forEach(name -> pools.add(new PoolResponse(name, totals.get(name),
                new BigDecimal(totals.get(name)).compareTo(FULL) == 0, shown.get(name))));
        return pools;
    }

    /** Gives the pool's members their cuts of its pot, when it has one and is balanced, and passes each member pool's
     *  cut down to it as that pool's pot. */
    private static void share(String name, BigDecimal pot, Map<String, List<PoolResponse.Member>> members,
            Map<String, String> totals, Map<String, List<PoolResponse.Member>> shown, int depth) {
        if (pot == null || depth > MAX_DEPTH || new BigDecimal(totals.get(name)).compareTo(FULL) != 0) {
            return;
        }
        List<PoolResponse.Member> withCuts = cuts(members.get(name), pot);
        shown.put(name, withCuts);
        for (PoolResponse.Member m : withCuts) {
            if (m.memberPool() != null && members.containsKey(m.memberPool())) {
                share(m.memberPool(), new BigDecimal(m.cut()), members, totals, shown, depth + 1);
            }
        }
    }

    /** The members with their cuts of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (a retired member cuts 0.00); the remainder, the pot minus the cuts added up, goes to the member with
     *  the largest share (the earliest added on a tie), so the cuts total exactly the pot. */
    static List<PoolResponse.Member> cuts(List<PoolResponse.Member> members, BigDecimal pot) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(FULL, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts[largest] = cuts[largest].add(pot.subtract(sum));
        }
        List<PoolResponse.Member> withCuts = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            withCuts.add(new PoolResponse.Member(m.personId(), m.memberPool(), m.name(), m.share(), m.retired(), cuts[i].toPlainString()));
        }
        return withCuts;
    }

    /** Whether the sheet has a pool of the name. */
    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the person is a member of the pool. */
    public boolean hasMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool_name = ? AND person_id = ?",
                Integer.class, sheetId, pool, personId);
        return n != null && n > 0;
    }

    /** Adds a pool to the sheet. */
    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    /** The pool the pool is a member of, or null when it is a member of none. */
    public String parentOf(String sheetId, String pool) {
        List<String> parents = jdbc.queryForList("SELECT pool_name FROM pool_member WHERE sheet_id = ? AND member_pool = ?",
                String.class, sheetId, pool);
        return parents.isEmpty() ? null : parents.get(0);
    }

    /** The pools that are members of the pool. */
    public List<String> subPoolsOf(String sheetId, String pool) {
        return jdbc.queryForList("SELECT member_pool FROM pool_member WHERE sheet_id = ? AND pool_name = ?"
                + " AND member_pool IS NOT NULL ORDER BY seq", String.class, sheetId, pool);
    }

    /** Adds the person (memberPool null) or the other pool (personId null) to the pool with the share. */
    public void addMember(String sheetId, String pool, Long personId, String memberPool, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, member_pool, share) VALUES (?, ?, ?, ?, ?)",
                sheetId, pool, personId, memberPool, share);
    }
}
