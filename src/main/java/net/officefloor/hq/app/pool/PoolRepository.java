package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    public boolean exists(String sheetId, String name) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return known != null && known > 0;
    }

    /** The person's name, or null when they are not on the sheet. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Add a pool; false when the sheet already has one of that name. */
    public boolean add(String sheetId, String name) {
        try {
            jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Add a member to a pool; false when the person is already in it. */
    public boolean addMember(String sheetId, String pool, long personId, BigDecimal share) {
        try {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share) VALUES (?, ?, ?, ?)",
                    sheetId, pool, personId, share);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Add another pool as a member of a pool; false when that pool is already a member of one. */
    public boolean addPoolMember(String sheetId, String pool, String memberPool, BigDecimal share) {
        try {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, member_pool, share) VALUES (?, ?, ?, ?)",
                    sheetId, pool, memberPool, share);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The pool the named pool is a member of, or null when it is a member of none. */
    public String parentOf(String sheetId, String pool) {
        List<String> found = jdbc.queryForList("SELECT pool_name FROM pool_member WHERE sheet_id = ? AND member_pool = ?",
                String.class, sheetId, pool);
        return found.isEmpty() ? null : found.get(0);
    }

    private List<String> childPools(String sheetId, String pool) {
        return jdbc.queryForList("SELECT member_pool FROM pool_member WHERE sheet_id = ? AND pool_name = ?"
                + " AND member_pool IS NOT NULL", String.class, sheetId, pool);
    }

    /** Whether {@code pool} is {@code other} or contains it through any chain of pool members. */
    public boolean containsOrIs(String sheetId, String pool, String other) {
        if (pool.equals(other)) return true;
        for (String child : childPools(sheetId, pool)) {
            if (containsOrIs(sheetId, child, other)) return true;
        }
        return false;
    }

    /** How many pools the chain from the top down to {@code pool} holds, the pool itself included. */
    public int depthAbove(String sheetId, String pool) {
        int depth = 1;
        for (String at = parentOf(sheetId, pool); at != null; at = parentOf(sheetId, at)) depth++;
        return depth;
    }

    /** How many pools the longest chain from {@code pool} down holds, the pool itself included. */
    public int heightBelow(String sheetId, String pool) {
        int below = 0;
        for (String child : childPools(sheetId, pool)) below = Math.max(below, heightBelow(sheetId, child));
        return below + 1;
    }

    /** The sheet's pot; null when there is none. */
    private BigDecimal pot(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT pot FROM sheet WHERE id = ?", (rs, i) -> rs.getBigDecimal("pot"),
                sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Each member's cut of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (0.00 for a retired member); the remainder, the pot minus the cuts added up, goes to the member
     *  with the largest share (the earliest added on a tie), so the cuts total exactly the pot. */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        int largest = -1;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal share = m.retired() ? BigDecimal.ZERO : new BigDecimal(m.share());
            BigDecimal cut = pot.multiply(share).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        List<PoolMember> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            cut.add(new PoolMember(m.personId(), m.pool(), m.name(), m.share(), m.retired(),
                    cuts.get(i).setScale(2).toPlainString()));
        }
        return cut;
    }

    /** The sheet's pools, by name, each with its members (people and pools) in the order they were added, and
     *  their cuts of the pool's pot when the pool is balanced and has a pot. A pool that is a member of none has
     *  the sheet's pot; a pool that is a member of another has its cut there as its pot (none when that pool
     *  shows no cuts). */
    public List<Pool> list(String sheetId) {
        Map<String, List<PoolMember>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY name", String.class,
                sheetId)) {
            members.put(name, new ArrayList<>());
        }
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT m.pool_name, m.person_id, m.member_pool, p.name, m.share, p.retired_from FROM pool_member m"
                + " LEFT JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<PoolMember> of = members.get(rs.getString("pool_name"));
            if (of == null) return;
            String share = rs.getBigDecimal("share").setScale(2).toPlainString();
            String memberPool = rs.getString("member_pool");
            if (memberPool != null) {
                parents.put(memberPool, rs.getString("pool_name"));
                of.add(new PoolMember(null, memberPool, memberPool, share, false, null));
            } else if (rs.getString("name") != null) {
                of.add(new PoolMember(rs.getLong("person_id"), null, rs.getString("name"), share,
                        rs.getDate("retired_from") != null, null));
            }
        }, sheetId);
        BigDecimal sheetPot = pot(sheetId);
        Map<String, Pool> done = new HashMap<>();
        List<Pool> pools = new ArrayList<>();
        for (String name : members.keySet()) {
            pools.add(resolve(name, members, parents, sheetPot, done));
        }
        return pools;
    }

    /** The named pool with its cuts, resolving the pool above it first for its pot. */
    private static Pool resolve(String name, Map<String, List<PoolMember>> members, Map<String, String> parents,
            BigDecimal sheetPot, Map<String, Pool> done) {
        Pool known = done.get(name);
        if (known != null) return known;
        BigDecimal pot = sheetPot;
        String parent = parents.get(name);
        if (parent != null) {
            pot = null;
            if (members.containsKey(parent)) {
                for (PoolMember m : resolve(parent, members, parents, sheetPot, done).members()) {
                    if (name.equals(m.pool()) && m.cut() != null) pot = new BigDecimal(m.cut());
                }
            }
        }
        List<PoolMember> of = members.get(name);
        BigDecimal total = of.stream().filter(m -> !m.retired()).map(m -> new BigDecimal(m.share()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        boolean balanced = total.compareTo(HUNDRED) == 0;
        Pool pool = new Pool(name, balanced && pot != null ? cut(of, pot) : of, total.toPlainString(), balanced);
        done.put(name, pool);
        return pool;
    }
}
