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
import net.officefloor.hq.app.access.Setting;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The pools of each sheet, in the order added, each name at most once on its sheet, and their members: people, and
 * other pools (each a member of at most one pool, never of itself through any chain, chains at most three deep).
 */
@Repository
public class PoolRepository {

    /** The most pools a chain of pools holds, the top one included. */
    public static final int MAX_DEPTH = 3;

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

    /** Adds the other pool to the pool; false when it is a member of a pool already. */
    public boolean addPoolMember(String sheetId, long poolId, long memberPoolId, BigDecimal share) {
        return jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, member_pool_id, share) SELECT ?, ?, ?, ?"
                + " WHERE NOT EXISTS (SELECT 1 FROM pool_member WHERE member_pool_id = ?)",
                sheetId, poolId, memberPoolId, share, memberPoolId) > 0;
    }

    /** The id of the pool the pool is a member of; null when none. */
    public Long parentOf(long poolId) {
        return jdbc.query("SELECT pool_id FROM pool_member WHERE member_pool_id = ?", (rs, n) -> rs.getLong(1), poolId)
                .stream().findFirst().orElse(null);
    }

    /** The pool and the pools above it, the pool first. */
    public List<Long> chainUp(long poolId) {
        List<Long> chain = new ArrayList<>();
        for (Long id = poolId; id != null && !chain.contains(id); id = parentOf(id)) {
            chain.add(id);
        }
        return chain;
    }

    /** How many pools deep the pool's longest chain down runs, the pool itself counting 1. */
    public int height(long poolId) {
        return height(poolId, 0);
    }

    private int height(long poolId, int guard) {
        if (guard > MAX_DEPTH) {
            return guard;
        }
        int below = 0;
        for (Long child : jdbc.queryForList("SELECT member_pool_id FROM pool_member WHERE pool_id = ?"
                + " AND member_pool_id IS NOT NULL", Long.class, poolId)) {
            below = Math.max(below, height(child, guard + 1));
        }
        return 1 + below;
    }

    /**
     * The sheet's pools, in the order added, each with its members in the order added, the total of their shares (a
     * member with a retirement date set, whatever the date, counts as 0.00) and whether it is exactly 100.00. A pool
     * that is a member of none has the sheet's pot; a member pool's pot is its cut in the pool above (none while that
     * has no cuts). Each member of a balanced pool with a pot has a cut: 0.00 when retired or held (a hold in force on
     * the sheet's today), else the pot times their share over the shares of the members with a cut added up, half to
     * even to two decimals, and the remainder to the member with a cut with the largest share (the earliest added on a
     * tie), so the cuts total the pot. A hold leaves the pool's total share and balance as they are.
     */
    public List<PoolResponse> list(String sheetId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        Map<Long, List<Long>> memberPools = new HashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            names.put(rs.getLong("id"), rs.getString("name"));
            members.put(rs.getLong("id"), new ArrayList<>());
            memberPools.put(rs.getLong("id"), new ArrayList<>());
        }, sheetId);
        Set<Long> held = new HashSet<>(jdbc.queryForList("SELECT DISTINCT h.person_id FROM person_hold h"
                + " JOIN person p ON p.id = h.person_id WHERE p.sheet_id = ? AND (h.from_date IS NULL OR h.from_date <= ?)"
                + " AND (h.to_date IS NULL OR h.to_date >= ?)", Long.class, sheetId, today(sheetId), today(sheetId)));
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, p.name, p.retired_from, q.name AS pool_name"
                + " FROM pool_member m LEFT JOIN person p ON p.id = m.person_id LEFT JOIN pool q ON q.id = m.member_pool_id"
                + " WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            long poolId = rs.getLong("pool_id");
            List<PoolResponse.Member> of = members.get(poolId);
            if (of == null) {
                return;
            }
            String share = rs.getBigDecimal("share").setScale(2).toPlainString();
            long memberPoolId = rs.getLong("member_pool_id");
            if (rs.wasNull()) {
                long personId = rs.getLong("person_id");
                of.add(new PoolResponse.Member(personId, null, rs.getString("name"), share,
                        rs.getDate("retired_from") != null, held.contains(personId), null));
                memberPools.get(poolId).add(null);
            } else {
                String name = rs.getString("pool_name");
                of.add(new PoolResponse.Member(null, name, name, share, false, false, null));
                memberPools.get(poolId).add(memberPoolId);
                parents.put(memberPoolId, poolId);
            }
        }, sheetId);
        String potValue = new SettingRepository(jdbc).find(sheetId, Setting.POT);
        BigDecimal pot = potValue == null ? null : new BigDecimal(potValue);
        Map<Long, String> totals = new HashMap<>();
        Map<Long, Boolean> balanced = new HashMap<>();
        names.keySet().forEach(id -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : members.get(id)) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            totals.put(id, total.toPlainString());
            balanced.put(id, total.compareTo(HUNDRED) == 0);
        });
        Map<Long, List<PoolResponse.Member>> cutOf = new HashMap<>();
        names.keySet().forEach(id -> cuts(id, pot, parents, members, memberPools, balanced, cutOf, 0));
        List<PoolResponse> pools = new ArrayList<>();
        names.forEach((id, name) -> pools.add(new PoolResponse(name, cutOf.get(id), totals.get(id), balanced.get(id))));
        return pools;
    }

    private java.sql.Date today(String sheetId) {
        return java.sql.Date.valueOf(new SheetRepository(jdbc).today(sheetId));
    }

    /** The pool's members with their cuts of its pot, worked out (and kept in {@code cutOf}) above it first. */
    private static List<PoolResponse.Member> cuts(long id, BigDecimal sheetPot, Map<Long, Long> parents,
            Map<Long, List<PoolResponse.Member>> members, Map<Long, List<Long>> memberPools, Map<Long, Boolean> balanced,
            Map<Long, List<PoolResponse.Member>> cutOf, int depth) {
        List<PoolResponse.Member> done = cutOf.get(id);
        if (done != null) {
            return done;
        }
        BigDecimal pot = sheetPot;
        Long parent = parents.get(id);
        if (parent != null) {
            pot = null;
            if (depth <= MAX_DEPTH) {
                List<PoolResponse.Member> above = cuts(parent, sheetPot, parents, members, memberPools, balanced, cutOf,
                        depth + 1);
                int at = memberPools.get(parent).indexOf(id);
                String cut = at < 0 ? null : above.get(at).cut();
                pot = cut == null ? null : new BigDecimal(cut);
            }
        }
        List<PoolResponse.Member> of = balanced.get(id) && pot != null ? cut(members.get(id), pot) : members.get(id);
        cutOf.put(id, of);
        return of;
    }

    /**
     * The members with their cuts of the pot, spread over the members with a cut (neither retired nor held) by their
     * shares; the remainder goes to the largest share among them, the earliest added on a tie.
     */
    private static List<PoolResponse.Member> cut(List<PoolResponse.Member> members, BigDecimal pot) {
        BigDecimal shares = BigDecimal.ZERO.setScale(2);
        for (PoolResponse.Member m : members) {
            if (m.hasCut()) {
                shares = shares.add(new BigDecimal(m.share()));
            }
        }
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            BigDecimal cut = !m.hasCut() || shares.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(new BigDecimal(m.share())).divide(shares, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (m.hasCut() && (largest < 0
                    || new BigDecimal(m.share()).compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0 && shares.signum() != 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        List<PoolResponse.Member> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            cut.add(members.get(i).withCut(cuts.get(i).setScale(2).toPlainString()));
        }
        return cut;
    }
}
