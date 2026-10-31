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

/** The pools of each sheet, in the order added, and each pool's members in the order added. */
@Repository
public class Pools {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    /** The most pools in a chain of pools, each a member of the one before. */
    public static final int MAX_DEPTH = 3;

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every pool on the sheet with its members, its total share and whether it is balanced. A retired
     *  member (a retirement date set, whatever the date) counts 0.00 toward the total. A member may be
     *  another pool. A pool that is a member of none has the sheet's pot; a pool that is a member of
     *  another has its cut there as its pot (none when the pool above is unbalanced or has none). Each
     *  member of a balanced pool with a pot has a cut of it (see {@link #cuts}); otherwise, none. */
    public List<PoolResponse> onSheet(String sheetId) {
        List<BigDecimal> pots = jdbc.query("SELECT pot FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getBigDecimal(1), sheetId);
        BigDecimal pot = pots.isEmpty() || pots.get(0) == null ? null : pots.get(0).setScale(2);
        Map<String, List<PoolMemberResponse>> members = new LinkedHashMap<>();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet_id = ? ORDER BY position", rs -> {
            members.put(rs.getString(1), new ArrayList<>());
            totals.put(rs.getString(1), BigDecimal.ZERO.setScale(2));
        }, sheetId);
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT m.pool, m.person_id, COALESCE(p.name, m.member_pool), m.share, p.retired_from IS NOT NULL, "
                + "m.member_pool FROM pool_member m LEFT JOIN person p ON p.id = m.person_id "
                + "WHERE m.sheet_id = ? ORDER BY m.position", rs -> {
                    String pool = rs.getString(1);
                    List<PoolMemberResponse> list = members.get(pool);
                    if (list == null) return;
                    String memberPool = rs.getString(6);
                    Long personId = memberPool == null ? rs.getLong(2) : null;
                    BigDecimal share = rs.getBigDecimal(4).setScale(2);
                    boolean retired = memberPool == null && rs.getBoolean(5);
                    list.add(new PoolMemberResponse(personId, rs.getString(3), share.toPlainString(), retired, null,
                            memberPool));
                    if (memberPool != null) parents.put(memberPool, pool);
                    if (!retired) totals.merge(pool, share, BigDecimal::add);
                }, sheetId);
        // Each pool's pot, from the pools that are a member of none down through their member pools.
        // The people with a hold in force on the sheet's today: they get no cut.
        Set<Long> held = new HashSet<>(jdbc.query("SELECT DISTINCT h.person_id FROM person_hold h "
                + "JOIN sheet s ON s.id = h.sheet_id WHERE h.sheet_id = ? AND h.from_date <= s.today "
                + "AND (h.to_date IS NULL OR h.to_date >= s.today)", (rs, i) -> rs.getLong(1), sheetId));
        Map<String, List<PoolMemberResponse>> shown = new LinkedHashMap<>();
        for (String name : members.keySet()) {
            if (!parents.containsKey(name)) share(name, pot, members, totals, held, shown, 0);
        }
        List<PoolResponse> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = totals.get(name);
            boolean balanced = total.compareTo(FULL) == 0;
            pools.add(new PoolResponse(name, shown.getOrDefault(name, list), total.toPlainString(), balanced));
        });
        return pools;
    }

    /** Shares the pool's pot (null for none) among its members, then each member pool's cut among its own. */
    private static void share(String name, BigDecimal pot, Map<String, List<PoolMemberResponse>> members,
            Map<String, BigDecimal> totals, Set<Long> held, Map<String, List<PoolMemberResponse>> shown, int depth) {
        if (shown.containsKey(name) || depth > MAX_DEPTH) return;
        List<PoolMemberResponse> list = members.get(name);
        boolean balanced = totals.get(name).compareTo(FULL) == 0;
        List<PoolMemberResponse> withCuts = balanced && pot != null ? cuts(list, pot, held) : list;
        shown.put(name, withCuts);
        for (PoolMemberResponse m : withCuts) {
            if (m.pool() != null && members.containsKey(m.pool())) {
                share(m.pool(), m.cut() == null ? null : new BigDecimal(m.cut()), members, totals, held, shown, depth + 1);
            }
        }
    }

    /** The members with their cuts of the pot. A retired member, or a person with a hold in force on
     *  the sheet's today, has no cut (0.00); each other member's is the pot times their share divided by
     *  the shares of the members with a cut added up, rounded half to even to two decimals. The
     *  remainder, the pot less the cuts, goes to the member with a cut with the largest share, the
     *  earliest added on a tie, so the cuts total the pot (when no member has a cut, each is 0.00). */
    private static List<PoolMemberResponse> cuts(List<PoolMemberResponse> members, BigDecimal pot, Set<Long> held) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        boolean[] cut = new boolean[members.size()];
        BigDecimal shares = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            cut[i] = !m.retired() && (m.personId() == null || !held.contains(m.personId()));
            if (cut[i]) shares = shares.add(new BigDecimal(m.share()));
        }
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = !cut[i] || shares.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (cut[i] && shares.signum() != 0 && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) cuts[largest] = cuts[largest].add(pot.subtract(sum));
        List<PoolMemberResponse> withCuts = new ArrayList<>(members.size());
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            withCuts.add(new PoolMemberResponse(m.personId(), m.name(), m.share(), m.retired(), cuts[i].toPlainString(),
                    m.pool()));
        }
        return withCuts;
    }

    /** Whether the sheet has a pool of that name. */
    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the person is a member of the pool. */
    public boolean hasMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool = ? AND person_id = ?",
                Integer.class, sheetId, pool, personId);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name, position) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM pool WHERE sheet_id = ?))",
                sheetId, name, sheetId);
    }

    /** The pool the pool is a member of, or null when none. */
    public String parentOf(String sheetId, String pool) {
        List<String> parents = jdbc.query("SELECT pool FROM pool_member WHERE sheet_id = ? AND member_pool = ?",
                (rs, i) -> rs.getString(1), sheetId, pool);
        return parents.isEmpty() ? null : parents.get(0);
    }

    /** The pools that are members of the pool. */
    public List<String> memberPools(String sheetId, String pool) {
        return jdbc.query("SELECT member_pool FROM pool_member WHERE sheet_id = ? AND pool = ? AND member_pool IS NOT NULL",
                (rs, i) -> rs.getString(1), sheetId, pool);
    }

    /** A member of the pool: a person (memberPool null) or another pool (personId null). */
    public void addMember(String sheetId, String pool, Long personId, String memberPool, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool, person_id, member_pool, share, position) VALUES (?, ?, ?, ?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM pool_member WHERE sheet_id = ?))",
                sheetId, pool, personId, memberPool, share, sheetId);
    }

    /** The name of the sheet's person with that id, or null when the sheet has none. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.query("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                (rs, i) -> rs.getString(1), sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }
}
