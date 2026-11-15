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
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewMember;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;
    private final SettingRepository settings;
    private final CrewRepository crews;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings, CrewRepository crews) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.crews = crews;
    }

    /** The sheet's pools in the order they were added, each with its members in the order they were added.
     *  A pool that is a member of none cuts the sheet's pot; one that is a member of another cuts its cut
     *  there (none when that pool has no cuts). After them come the sheet's crews, each a pool of its own
     *  (see {@link #crewPools}). Each member carries their cut at the end of every period whose to-date is
     *  on or before the sheet's today (see {@link #asOf}). */
    public List<Pool> list(String sheetId) {
        List<Pool> pools = asOf(sheetId, null);
        Map<String, Map<String, String>> history = new HashMap<>();
        Map<String, LocalDate> periods = new LinkedHashMap<>();
        jdbc.query("SELECT p.name, p.to_date FROM period p JOIN sheet s ON s.id = p.sheet_id "
                + "WHERE p.sheet_id = ? AND p.to_date <= s.today ORDER BY p.from_date, p.id",
                rs -> { periods.put(rs.getString("name"), rs.getDate("to_date").toLocalDate()); }, sheetId);
        for (Map.Entry<String, LocalDate> period : periods.entrySet()) {
            for (Pool pool : asOf(sheetId, period.getValue())) {
                for (PoolMember m : pool.members()) {
                    history.computeIfAbsent(key(pool, m), k -> new HashMap<>()).put(period.getKey(), m.cut());
                }
            }
        }
        List<Pool> result = new ArrayList<>();
        for (Pool pool : pools) {
            List<PoolMember> members = new ArrayList<>();
            for (PoolMember m : pool.members()) {
                Map<String, String> then = history.getOrDefault(key(pool, m), Map.of());
                Map<String, String> cuts = new LinkedHashMap<>();
                for (String period : periods.keySet()) cuts.put(period, then.getOrDefault(period, ""));
                members.add(m.withPeriodCuts(cuts));
            }
            result.add(new Pool(pool.id(), pool.sheetId(), pool.name(), members, pool.totalShare(), pool.balanced(),
                    pool.parentId(), pool.crew()));
        }
        return result;
    }

    /** The member's place: the pool and the person or pool that is the member. */
    private static String key(Pool pool, PoolMember m) {
        return pool.id() + (m.poolId() != null ? ":pool:" + m.poolId() : ":person:" + m.personId());
    }

    /** The sheet's pools (crews' included) with their cuts as they stood on the date with it as the sheet's
     *  today (null: as they now stand): the pools, members, crews and holds added while the sheet's today was
     *  on or before it (or before that was kept), the pot and retirements as last set by then. */
    private List<Pool> asOf(String sheetId, LocalDate asOf) {
        java.sql.Date on = asOf == null ? null : java.sql.Date.valueOf(asOf);
        java.sql.Date added = java.sql.Date.valueOf(asOf == null ? LocalDate.of(9999, 12, 31) : asOf);
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? AND COALESCE(added_on, DATE '0001-01-01') <= ? "
                + "ORDER BY id", rs -> {
            names.put(rs.getLong("id"), rs.getString("name"));
            members.put(rs.getLong("id"), new ArrayList<>());
        }, sheetId, added);
        Set<Long> retired = retired(sheetId, asOf);
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, COALESCE(p.name, q.name) AS name, m.share, "
                + "m.from_date, m.to_date, CASE WHEN (m.from_date IS NULL OR m.from_date <= COALESCE(CAST(? AS DATE), s.today)) "
                + "AND (m.to_date IS NULL OR m.to_date >= COALESCE(CAST(? AS DATE), s.today)) THEN TRUE ELSE FALSE END AS current "
                + "FROM pool_member m JOIN sheet s ON s.id = m.sheet_id LEFT JOIN person p ON p.id = m.person_id "
                + "LEFT JOIN pool q ON q.id = m.member_pool_id WHERE m.sheet_id = ? "
                + "AND COALESCE(m.added_on, DATE '0001-01-01') <= ? ORDER BY m.id", rs -> {
                    List<PoolMember> of = members.get(rs.getLong("pool_id"));
                    if (of != null) {
                        Long personId = rs.getObject("person_id") == null ? null : rs.getLong("person_id");
                        Long poolId = rs.getObject("member_pool_id") == null ? null : rs.getLong("member_pool_id");
                        if (poolId != null && !names.containsKey(poolId)) return;
                        if (poolId != null) parents.put(poolId, rs.getLong("pool_id"));
                        of.add(new PoolMember(personId, poolId, rs.getString("name"),
                                rs.getBigDecimal("share").setScale(2).toPlainString(),
                                personId != null && retired.contains(personId),
                                rs.getObject("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString(),
                                rs.getObject("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString(),
                                rs.getBoolean("current"), "", Map.of()));
                    }
                }, on, on, sheetId, added);
        BigDecimal pot = asOf == null ? settings.pot(sheetId) : potAsOf(sheetId, on);
        Set<Long> held = new HashSet<>(jdbc.queryForList("SELECT DISTINCT h.person_id FROM person_hold h "
                + "JOIN sheet s ON s.id = h.sheet_id WHERE h.sheet_id = ? "
                + "AND h.from_date <= COALESCE(CAST(? AS DATE), s.today) "
                + "AND (h.to_date IS NULL OR h.to_date >= COALESCE(CAST(? AS DATE), s.today)) "
                + "AND COALESCE(h.added_on, DATE '0001-01-01') <= ?", Long.class, sheetId, on, on, added));
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Map.Entry<Long, List<PoolMember>> pool : members.entrySet()) {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : pool.getValue()) {
                if (m.current()) total = total.add(new BigDecimal(m.share()));
            }
            totals.put(pool.getKey(), total);
        }
        Map<Long, List<PoolMember>> cut = new HashMap<>();
        for (Long id : names.keySet()) cutOf(id, members, parents, totals, held, pot, cut, new HashSet<>());
        List<Pool> pools = new ArrayList<>();
        for (Map.Entry<Long, String> pool : names.entrySet()) {
            BigDecimal total = totals.get(pool.getKey());
            pools.add(new Pool(pool.getKey(), sheetId, pool.getValue(), cut.get(pool.getKey()),
                    total.toPlainString(), total.compareTo(FULL) == 0, parents.get(pool.getKey()), false));
        }
        pools.addAll(crewPools(sheetId, asOf, pot, held, retired));
        return pools;
    }

    /** The sheet's retired people as of the date (null: now): those whose retirement last set by then (with
     *  the sheet's today on or before it) stands. */
    private Set<Long> retired(String sheetId, LocalDate asOf) {
        if (asOf == null) {
            return new HashSet<>(jdbc.queryForList(
                    "SELECT id FROM person WHERE sheet_id = ? AND retired_from IS NOT NULL", Long.class, sheetId));
        }
        Map<Long, Boolean> last = new HashMap<>();
        jdbc.query("SELECT person_id, retired_from FROM person_retired_history WHERE sheet_id = ? "
                + "AND (on_date IS NULL OR on_date <= ?) ORDER BY id", rs -> {
                    last.put(rs.getLong("person_id"), rs.getObject("retired_from") != null);
                }, sheetId, java.sql.Date.valueOf(asOf));
        Set<Long> retired = new HashSet<>();
        last.forEach((id, is) -> {
            if (is) retired.add(id);
        });
        return retired;
    }

    /** The sheet's pot as last set while the sheet's today was on or before the date; null when none. */
    private BigDecimal potAsOf(String sheetId, java.sql.Date on) {
        List<String> found = jdbc.query("SELECT val FROM setting_history WHERE sheet_id = ? AND setting = ? "
                + "AND (on_date IS NULL OR on_date <= ?) ORDER BY id DESC LIMIT 1",
                (rs, n) -> rs.getString("val"), sheetId, SettingRepository.POT, on);
        String value = found.isEmpty() || found.get(0) == null ? "" : found.get(0).trim();
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    /** The sheet's crews in the order they were added, each as a pool under its name (id: the crew's id
     *  negated; a member of no pool, and taking none): the lead then the members in the order added, at
     *  equal shares of 100.00 as a person's patrons' shares are (the remainder on the lead), cutting the
     *  sheet's pot like any pool. A crew without a lead (they left the sheet) is its members alone; with
     *  none either, it has no members and a total of 0.00. */
    private List<Pool> crewPools(String sheetId, LocalDate asOf, BigDecimal pot, Set<Long> held, Set<Long> retired) {
        List<Pool> pools = new ArrayList<>();
        for (Crew crew : crews.list(sheetId, asOf)) {
            List<Long> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            if (crew.leadId() != null) {
                ids.add(crew.leadId());
                names.add(crew.lead());
            }
            for (CrewMember m : crew.members()) {
                ids.add(m.personId());
                names.add(m.name());
            }
            long hundredths = FULL.movePointRight(2).longValueExact();
            if (ids.isEmpty()) {
                pools.add(new Pool(-crew.id(), sheetId, crew.name(), List.of(), "0.00", false, null, true));
                continue;
            }
            long each = hundredths / ids.size();
            List<PoolMember> members = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                long share = i == 0 ? hundredths - each * (ids.size() - 1) : each;
                members.add(new PoolMember(ids.get(i), null, names.get(i),
                        BigDecimal.valueOf(share, 2).toPlainString(), retired.contains(ids.get(i)), null, null, true, "",
                        Map.of()));
            }
            pools.add(new Pool(-crew.id(), sheetId, crew.name(), pot != null ? cut(members, pot, held) : members,
                    FULL.toPlainString(), true, null, true));
        }
        return pools;
    }

    /** The pool's members with their cuts, worked out (and kept in {@code cut}) after those of the pool above
     *  it: its pot is the sheet's pot when it is a member of none, else its cut in the pool above. */
    private static List<PoolMember> cutOf(Long id, Map<Long, List<PoolMember>> members, Map<Long, Long> parents,
            Map<Long, BigDecimal> totals, Set<Long> held, BigDecimal sheetPot, Map<Long, List<PoolMember>> cut, Set<Long> seen) {
        List<PoolMember> done = cut.get(id);
        if (done != null) return done;
        List<PoolMember> of = members.get(id);
        BigDecimal pot = null;
        Long parent = parents.get(id);
        if (parent == null) {
            pot = sheetPot;
        } else if (seen.add(id) && members.containsKey(parent)) {
            for (PoolMember m : cutOf(parent, members, parents, totals, held, sheetPot, cut, seen)) {
                if (id.equals(m.poolId()) && !m.cut().isEmpty()) pot = new BigDecimal(m.cut());
            }
        }
        List<PoolMember> result = pot != null && totals.get(id).compareTo(FULL) == 0 ? cut(of, pot, held) : of;
        cut.put(id, result);
        return result;
    }

    /** The members with their cuts of the pot. A member with a cut is one neither retired nor on a hold in
     *  force on the sheet's today ({@code held}); the others' cut is 0.00, but a member whose dates do not
     *  include the sheet's today is left out entirely (cut ""), as is every member when each current member
     *  is retired (the pool shows no cuts). Each member with a cut gets the pot
     *  times their share over the shares of the members with a cut added up, rounded half to even to two
     *  decimals; the remainder goes to the member with the smallest non-zero cut (the latest added on a tie; a
     *  member with no cut gets none), or to the last member with a cut and a share when no cut is non-zero, so the cuts total exactly the pot (all 0.00 when no member has a cut). */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot, Set<Long> held) {
        if (members.stream().anyMatch(PoolMember::current)
                && members.stream().filter(PoolMember::current).allMatch(PoolMember::retired)) {
            return members.stream().map(m -> m.withCut("")).toList();
        }
        BigDecimal[] cuts = new BigDecimal[members.size()];
        boolean[] cutting = new boolean[members.size()];
        BigDecimal shares = BigDecimal.ZERO.setScale(2);
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            cutting[i] = m.current() && !m.retired() && (m.personId() == null || !held.contains(m.personId()));
            if (cutting[i]) shares = shares.add(new BigDecimal(m.share()));
        }
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int smallest = -1;
        int last = -1;
        for (int i = 0; i < members.size(); i++) {
            BigDecimal share = new BigDecimal(members.get(i).share());
            cuts[i] = cutting[i] ? pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(2);
            sum = sum.add(cuts[i]);
            if (cutting[i] && share.signum() != 0) last = i;
            if (cutting[i] && cuts[i].signum() != 0
                    && (smallest < 0 || cuts[i].compareTo(cuts[smallest]) <= 0)) {
                smallest = i;
            }
        }
        int to = smallest >= 0 ? smallest : last;
        if (to >= 0) cuts[to] = cuts[to].add(pot.subtract(sum));
        List<PoolMember> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            cut.add(members.get(i).withCut(members.get(i).current() ? cuts[i].toPlainString() : ""));
        }
        return cut;
    }

    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** The pool's name, or null when there is no such pool on the sheet. */
    public String nameOf(String sheetId, long poolId) {
        List<String> found = jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, poolId);
        return found.isEmpty() ? null : found.get(0);
    }

    public boolean hasMember(long poolId, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, poolId, personId);
        return n != null && n > 0;
    }

    /** The pool the given pool is a member of, or null when it is a member of none. */
    public Long parentOf(long poolId) {
        List<Long> found = jdbc.queryForList("SELECT pool_id FROM pool_member WHERE member_pool_id = ?",
                Long.class, poolId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The pools that are members of the given pool. */
    public List<Long> childrenOf(long poolId) {
        return jdbc.queryForList("SELECT member_pool_id FROM pool_member WHERE pool_id = ? "
                + "AND member_pool_id IS NOT NULL ORDER BY id", Long.class, poolId);
    }

    public void addPoolMember(String sheetId, long poolId, long memberPoolId, BigDecimal share, String from,
            String to, String by) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, member_pool_id, share, from_date, to_date, added_by, added_on) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, poolId, memberPoolId,
                share, date(from), date(to), by, sheetId);
    }

    public void add(String sheetId, String name, String by) {
        jdbc.update("INSERT INTO pool (sheet_id, name, added_by, added_on) VALUES (?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", sheetId, name, by, sheetId);
    }

    public void addMember(String sheetId, long poolId, long personId, BigDecimal share, String from, String to,
            String by) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share, from_date, to_date, added_by, added_on) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, poolId, personId,
                share, date(from), date(to), by, sheetId);
    }

    private static java.sql.Date date(String value) {
        return value == null ? null : java.sql.Date.valueOf(value);
    }

    /** Move the person {@code fromId}'s pool memberships to {@code intoId}: their share added to
     *  into's where into is already a member. */
    public void moveMembers(String sheetId, long fromId, long intoId) {
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f "
                + "WHERE f.pool_id = m.pool_id AND f.person_id = ?) WHERE m.sheet_id = ? AND m.person_id = ? "
                + "AND m.pool_id IN (SELECT f.pool_id FROM pool_member f WHERE f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ? AND pool_id IN "
                + "(SELECT m.pool_id FROM pool_member m WHERE m.person_id = ?)", sheetId, fromId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
    }
}
