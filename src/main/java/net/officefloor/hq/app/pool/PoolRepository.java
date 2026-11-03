package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** Whether the sheet has a crew of that name (a crew is also a pool of its name). */
    public boolean crewExists(String sheetId, String name) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?",
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
    public boolean addMember(String sheetId, String pool, long personId, BigDecimal share, LocalDate from,
            LocalDate to) {
        try {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share, from_date, to_date)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", sheetId, pool, personId, share, from, to);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Add another pool as a member of a pool; false when that pool is already a member of one. */
    public boolean addPoolMember(String sheetId, String pool, String memberPool, BigDecimal share, LocalDate from,
            LocalDate to) {
        try {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, member_pool, share, from_date, to_date)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", sheetId, pool, memberPool, share, from, to);
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

    /** The ids of the sheet's people with a hold in force on the sheet's today (a hold whose dates include it). */
    private Set<Long> heldToday(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT DISTINCT h.person_id FROM person_hold h"
                + " JOIN sheet s ON s.id = h.sheet_id WHERE h.sheet_id = ?"
                + " AND (h.from_date IS NULL OR h.from_date <= COALESCE(s.today, CURRENT_DATE))"
                + " AND (h.to_date IS NULL OR h.to_date >= COALESCE(s.today, CURRENT_DATE))", Long.class, sheetId));
    }

    /** Each member's cut of the pot (null for a member that is not current). A member with a cut is one current,
     *  neither retired nor held on the sheet's today;
     *  each such member's cut is the pot times their share divided by the shares of the members with a cut added
     *  up, rounded half to even to two decimals (0.00 for any other member, and for every member when none has a
     *  cut); the remainder, the pot minus the cuts added up, goes to the member with the smallest non-zero cut
     *  (the latest added on a tie; a member with no cut gets none), or when no cut is non-zero to the last member
     *  with a share, so the cuts total exactly the pot. */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot, Set<Long> held) {
        List<Boolean> withCut = new ArrayList<>();
        BigDecimal shares = BigDecimal.ZERO;
        for (PoolMember m : members) {
            boolean has = m.current() && !m.retired() && (m.personId() == null || !held.contains(m.personId()));
            withCut.add(has);
            if (has) shares = shares.add(new BigDecimal(m.share()));
        }
        List<BigDecimal> cuts = new ArrayList<>();
        int smallest = -1;
        int lastWithShare = -1;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            boolean has = withCut.get(i) && shares.signum() > 0;
            BigDecimal share = has ? new BigDecimal(m.share()) : BigDecimal.ZERO;
            BigDecimal cut = has ? pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(2);
            cuts.add(cut);
            sum = sum.add(cut);
            if (has && share.signum() > 0) lastWithShare = i;
            if (has && cut.signum() != 0 && (smallest < 0 || cut.compareTo(cuts.get(smallest)) <= 0)) {
                smallest = i;
            }
        }
        int to = smallest >= 0 ? smallest : lastWithShare;
        if (to >= 0) cuts.set(to, cuts.get(to).add(pot.subtract(sum)));
        List<PoolMember> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            cut.add(new PoolMember(m.personId(), m.pool(), m.name(), m.share(), m.retired(),
                    m.current() ? cuts.get(i).setScale(2).toPlainString() : null, m.from(), m.to(), m.current()));
        }
        return cut;
    }

    /** Shares (two decimals) for a crew's people, lead first: 100.00 split equally as patrons' shares are, each
     *  rounded half to even, the remainder (100.00 less the shares added up) given to the lead. */
    private static List<BigDecimal> crewShares(int count) {
        List<BigDecimal> shares = new ArrayList<>();
        if (count == 0) return shares;
        BigDecimal each = HUNDRED.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        for (int i = 0; i < count; i++) shares.add(each);
        shares.set(0, each.add(HUNDRED.subtract(each.multiply(BigDecimal.valueOf(count)))));
        return shares;
    }

    /** The sheet's crews as pools, by name: each crew's lead then its members (people still on the sheet) in the
     *  order they were added, at equal shares; always current, never dated. */
    private Map<String, List<PoolMember>> crewPools(String sheetId) {
        Map<String, List<PoolMember>> people = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_id, p.name AS lead, p.retired_from FROM crew c"
                + " LEFT JOIN person p ON p.id = c.lead_id WHERE c.sheet_id = ? ORDER BY c.id", rs -> {
            List<PoolMember> of = new ArrayList<>();
            if (rs.getString("lead") != null) {
                of.add(new PoolMember(rs.getLong("lead_id"), null, rs.getString("lead"), null,
                        rs.getDate("retired_from") != null, null, null, null, true));
            }
            people.put(rs.getString("name"), of);
        }, sheetId);
        jdbc.query("SELECT m.crew_name, m.person_id, p.name, p.retired_from FROM crew_member m"
                + " JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<PoolMember> of = people.get(rs.getString("crew_name"));
            if (of != null) of.add(new PoolMember(rs.getLong("person_id"), null, rs.getString("name"), null,
                    rs.getDate("retired_from") != null, null, null, null, true));
        }, sheetId);
        Map<String, List<PoolMember>> crews = new LinkedHashMap<>();
        for (Map.Entry<String, List<PoolMember>> crew : people.entrySet()) {
            List<BigDecimal> shares = crewShares(crew.getValue().size());
            List<PoolMember> of = new ArrayList<>();
            for (int i = 0; i < shares.size(); i++) {
                PoolMember m = crew.getValue().get(i);
                of.add(new PoolMember(m.personId(), null, m.name(), shares.get(i).toPlainString(), m.retired(), null,
                        null, null, true));
            }
            crews.put(crew.getKey(), of);
        }
        return crews;
    }

    /** The sheet's pools, by name, each with its members (people and pools) in the order they were added, and
     *  their cuts of the pool's pot when the pool is balanced, has a pot and has a current member not retired. A
     *  pool's total share counts every current member's share, a retired member's included. A pool that is a member of none has
     *  the sheet's pot; a pool that is a member of another has its cut there as its pot (none when that pool
     *  shows no cuts). Each crew is also a pool of its name (its lead and members at equal shares), placed after
     *  the pools in the order pools were added, crews in the order they were added. */
    public List<Pool> list(String sheetId) {
        Map<String, List<PoolMember>> members = new LinkedHashMap<>();
        Map<String, Integer> added = new HashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY added", String.class,
                sheetId)) {
            added.put(name, added.size());
        }
        Map<String, List<PoolMember>> crews = crewPools(sheetId);
        for (String name : crews.keySet()) {
            added.putIfAbsent(name, added.size());
        }
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? UNION SELECT name FROM crew"
                + " WHERE sheet_id = ? ORDER BY name", String.class, sheetId, sheetId)) {
            members.put(name, crews.containsKey(name) ? crews.get(name) : new ArrayList<>());
        }
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT m.pool_name, m.person_id, m.member_pool, p.name, m.share, p.retired_from, m.from_date,"
                + " m.to_date, (m.from_date IS NULL OR m.from_date <= COALESCE(s.today, CURRENT_DATE))"
                + " AND (m.to_date IS NULL OR m.to_date >= COALESCE(s.today, CURRENT_DATE)) AS is_current"
                + " FROM pool_member m JOIN sheet s ON s.id = m.sheet_id"
                + " LEFT JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<PoolMember> of = members.get(rs.getString("pool_name"));
            if (of == null || crews.containsKey(rs.getString("pool_name"))) return;
            String share = rs.getBigDecimal("share").setScale(2).toPlainString();
            String memberPool = rs.getString("member_pool");
            java.sql.Date fromDate = rs.getDate("from_date");
            java.sql.Date toDate = rs.getDate("to_date");
            String from = fromDate == null ? null : fromDate.toLocalDate().toString();
            String to = toDate == null ? null : toDate.toLocalDate().toString();
            boolean current = rs.getBoolean("is_current");
            if (memberPool != null) {
                parents.put(memberPool, rs.getString("pool_name"));
                of.add(new PoolMember(null, memberPool, memberPool, share, false, null, from, to, current));
            } else if (rs.getString("name") != null) {
                of.add(new PoolMember(rs.getLong("person_id"), null, rs.getString("name"), share,
                        rs.getDate("retired_from") != null, null, from, to, current));
            }
        }, sheetId);
        BigDecimal sheetPot = pot(sheetId);
        Set<Long> held = heldToday(sheetId);
        Map<String, Pool> done = new HashMap<>();
        List<Pool> pools = new ArrayList<>();
        for (String name : members.keySet()) {
            pools.add(resolve(name, members, parents, sheetPot, held, done).withAdded(added.get(name)));
        }
        return pools;
    }

    /** The named pool with its cuts, resolving the pool above it first for its pot. */
    private static Pool resolve(String name, Map<String, List<PoolMember>> members, Map<String, String> parents,
            BigDecimal sheetPot, Set<Long> held, Map<String, Pool> done) {
        Pool known = done.get(name);
        if (known != null) return known;
        BigDecimal pot = sheetPot;
        String parent = parents.get(name);
        if (parent != null) {
            pot = null;
            if (members.containsKey(parent)) {
                for (PoolMember m : resolve(parent, members, parents, sheetPot, held, done).members()) {
                    if (name.equals(m.pool()) && m.cut() != null) pot = new BigDecimal(m.cut());
                }
            }
        }
        List<PoolMember> of = members.get(name);
        BigDecimal total = of.stream().filter(PoolMember::current).map(m -> new BigDecimal(m.share()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        boolean balanced = total.compareTo(HUNDRED) == 0;
        boolean allRetired = of.stream().filter(PoolMember::current).allMatch(PoolMember::retired);
        Pool pool = new Pool(name, balanced && pot != null && !allRetired ? cut(of, pot, held) : of,
                total.toPlainString(), balanced, 0);
        done.put(name, pool);
        return pool;
    }
}
