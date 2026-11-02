package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools on each sheet and their members. */
@Repository
public class Pools {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    private final Settings settings;

    private final SheetClock clock;

    private final Periods periods;

    public Pools(JdbcTemplate jdbc, Settings settings, SheetClock clock, Periods periods) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.clock = clock;
        this.periods = periods;
    }

    /**
     * Every pool on the sheet in the order added, its members (people and pools) in the order added, each with their
     * cut of the pool's pot: a pool that is a member of none has the sheet's pot; a pool that is a member of another
     * has its cut there as its pot (none when that pool is unbalanced or has none). A person with a hold in force on
     * the sheet's today gets no cut, their share spread over the other members with a cut, and so does a retired
     * person, whose share still counts in the total share. A member whose membership dates do not include the sheet's
     * today is left out entirely: not in the total share, no cut. Each crew is also a pool, after the pools added, of its
     * name: its lead and then its members in the order added, at equal shares (see {@link #crewShares}). Each member
     * also has, for every period whose to-date is on or before the sheet's today, their cut as it stood at the end of
     * that period (see {@link #asAt}).
     */
    public List<Pool> list(String sheet) {
        LocalDate now = clock.today(sheet);
        List<Pool> pools = list(sheet, now, settings.pot(sheet), null);
        Map<String, Map<String, String>> cuts = new HashMap<>();
        for (Period period : periods.list(sheet)) {
            LocalDate end = LocalDate.parse(period.to());
            if (end.isAfter(now)) {
                continue;
            }
            for (Pool pool : asAt(sheet, end)) {
                for (PoolMember m : pool.members()) {
                    cuts.computeIfAbsent(pool.name() + "\n" + m.key(), (k) -> new LinkedHashMap<>())
                            .put(period.name(), m.cut());
                }
            }
        }
        return pools.stream().map((pool) -> new Pool(pool.name(), pool.members().stream()
                .map((m) -> new PoolMember(m.person(), m.memberPool(), m.name(), m.share(), m.retired(), m.from(),
                        m.to(), m.current(), m.cut(), cuts.getOrDefault(pool.name() + "\n" + m.key(), Map.of())))
                .toList(), pool.totalShare(), pool.balanced())).toList();
    }

    /**
     * The pools as they stood at the end of the day, with it as the sheet's today: only the pools and members added
     * (on a sheet's today) on or before it, a person retired when their retirement date is on or before it, the holds
     * in force on it and the pot as it stood then.
     */
    private List<Pool> asAt(String sheet, LocalDate day) {
        return list(sheet, day, settings.potOn(sheet, day), day);
    }

    /** The pools as of the day with the pot; when asAt is given, only what was added by then (see {@link #asAt}). */
    private List<Pool> list(String sheet, LocalDate now, BigDecimal pot, LocalDate asAt) {
        Date at = asAt == null ? null : Date.valueOf(asAt);
        String added = asAt == null ? "" : " AND (added_on IS NULL OR added_on <= ?)";
        Object[] args = asAt == null ? new Object[] { sheet } : new Object[] { sheet, at };
        Map<String, List<Ordered>> members = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet = ?" + added + " ORDER BY position",
                (rs) -> {
                    members.put(rs.getString("name"), new ArrayList<>());
                }, args);
        jdbc.query("SELECT m.pool, m.person_id, p.name, m.share, p.retired_from, m.position, m.from_date, m.to_date"
                + " FROM pool_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ?"
                + added.replace("added_on", "m.added_on"),
                (rs) -> {
                    List<Ordered> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        Date retired = rs.getDate("retired_from");
                        list.add(new Ordered(rs.getInt("position"), new PoolMember(rs.getLong("person_id"), null,
                                rs.getString("name"), rs.getBigDecimal("share").setScale(2).toPlainString(),
                                retired != null && (at == null || !retired.after(at)), dateOf(rs, "from_date"),
                                dateOf(rs, "to_date"), current(rs, now), "")));
                    }
                }, args);
        crewPools(sheet, members);
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT pool, member, share, position, from_date, to_date FROM pool_sub WHERE sheet = ?" + added,
                (rs) -> {
                    List<Ordered> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        list.add(new Ordered(rs.getInt("position"), new PoolMember(0, rs.getString("member"),
                                rs.getString("member"), rs.getBigDecimal("share").setScale(2).toPlainString(), false,
                                dateOf(rs, "from_date"), dateOf(rs, "to_date"), current(rs, now), "")));
                        parents.put(rs.getString("member"), rs.getString("pool"));
                    }
                }, args);
        Map<String, Pool> pools = new LinkedHashMap<>();
        members.forEach((name, ordered) -> {
            ordered.sort(Comparator.comparingInt(Ordered::position));
            List<PoolMember> list = ordered.stream().map(Ordered::member).toList();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : list) {
                if (m.current()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            pools.put(name, new Pool(name, list, total.toPlainString(), total.compareTo(FULL) == 0));
        });
        // The people with a hold whose dates include the sheet's today.
        Date today = Date.valueOf(now);
        Set<Long> held = new HashSet<>(jdbc.queryForList("SELECT DISTINCT h.person_id FROM person_hold h"
                + " JOIN person p ON p.id = h.person_id WHERE p.sheet = ? AND h.from_date <= ?"
                + " AND (h.to_date IS NULL OR h.to_date >= ?)", Long.class, sheet, today, today));
        // Top down: a pool's pot is the sheet's when it is a member of none, else its cut in the pool above.
        Map<String, Pool> cut = new HashMap<>();
        for (String name : pools.keySet()) {
            cutDown(name, pools, parents, pot, held, cut);
        }
        return pools.keySet().stream().map(cut::get).toList();
    }

    private record Ordered(int position, PoolMember member) {
    }

    /** Adds each crew as a pool: its lead (when it still has one) first, then its members in the order added, at {@link #crewShares}. */
    private void crewPools(String sheet, Map<String, List<Ordered>> members) {
        Map<String, List<PoolMember>> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, c.lead_id, p.name AS person_name, p.retired_from FROM crew c"
                + " LEFT JOIN person p ON p.sheet = c.sheet AND p.id = c.lead_id WHERE c.sheet = ? ORDER BY c.position",
                (rs) -> {
                    List<PoolMember> list = new ArrayList<>();
                    // A crew whose lead has gone (e.g. transferred away) is still a pool, of its members only.
                    if (rs.getString("person_name") != null) {
                        list.add(crewMember(rs, "lead_id"));
                    }
                    crews.put(rs.getString("name"), list);
                }, sheet);
        jdbc.query("SELECT m.crew, m.person_id, p.name AS person_name, p.retired_from FROM crew_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ? ORDER BY m.position",
                (rs) -> {
                    List<PoolMember> list = crews.get(rs.getString("crew"));
                    if (list != null) {
                        list.add(crewMember(rs, "person_id"));
                    }
                }, sheet);
        crews.forEach((name, list) -> {
            if (members.containsKey(name)) {
                return;
            }
            List<BigDecimal> shares = crewShares(list.size());
            List<Ordered> ordered = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PoolMember m = list.get(i);
                ordered.add(new Ordered(i, new PoolMember(m.person(), null, m.name(), shares.get(i).toPlainString(),
                        m.retired(), "", "", true, "")));
            }
            members.put(name, ordered);
        });
    }

    private static PoolMember crewMember(ResultSet rs, String id) throws SQLException {
        return new PoolMember(rs.getLong(id), null, rs.getString("person_name"), "", rs.getDate("retired_from") != null,
                "", "", true, "");
    }

    /**
     * Shares for a crew of this many (lead first), as patrons' shares are: 100.00 split equally, each rounded half to
     * even to two decimals, with the remainder (100.00 less the shares added up) on the first, the lead.
     */
    static List<BigDecimal> crewShares(int count) {
        List<BigDecimal> shares = new ArrayList<>();
        if (count == 0) {
            return shares;
        }
        BigDecimal each = FULL.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        for (int i = 0; i < count; i++) {
            shares.add(each);
        }
        shares.set(0, each.add(FULL.subtract(each.multiply(BigDecimal.valueOf(count)))));
        return shares;
    }

    /** The date in the column, or "" when none. */
    private static String dateOf(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? "" : date.toLocalDate().toString();
    }

    /** Whether the membership's dates include the sheet's today. */
    private static boolean current(ResultSet rs, LocalDate today) throws SQLException {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return (from == null || !from.toLocalDate().isAfter(today)) && (to == null || !to.toLocalDate().isBefore(today));
    }

    /** The pool with its members' cuts, its pool above cut first. */
    private static Pool cutDown(String name, Map<String, Pool> pools, Map<String, String> parents, BigDecimal sheetPot,
            Set<Long> held, Map<String, Pool> done) {
        Pool known = done.get(name);
        if (known != null) {
            return known;
        }
        Pool pool = pools.get(name);
        BigDecimal pot = sheetPot;
        String parent = parents.get(name);
        if (parent != null && pools.containsKey(parent)) {
            pot = null;
            for (PoolMember m : cutDown(parent, pools, parents, sheetPot, held, done).members()) {
                if (name.equals(m.memberPool()) && !m.cut().isEmpty()) {
                    pot = new BigDecimal(m.cut());
                }
            }
        }
        Pool out = pool.balanced() && pot != null
                ? new Pool(name, cut(pool.members(), pot, held), pool.totalShare(), true)
                : pool;
        done.put(name, out);
        return out;
    }

    /**
     * The members each with their cut of the pot: the pot times their share over the shares of the members with a cut
     * (neither retired nor held), half to even to two decimals (a retired or held member's 0.00); the remainder, the
     * pot less the cuts, goes to the member with the smallest non-zero cut, the latest added on a tie (or, when no
     * cut is non-zero, to the last member with a cut and a share), so the cuts total the pot (each 0.00 when no member has a cut). A pool whose every member within their dates is retired
     * shows no cuts.
     */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot, Set<Long> held) {
        if (members.stream().filter(PoolMember::current).allMatch(PoolMember::retired)) {
            return members;
        }
        BigDecimal shares = BigDecimal.ZERO.setScale(2);
        for (PoolMember m : members) {
            if (hasCut(m, held)) {
                shares = shares.add(new BigDecimal(m.share()));
            }
        }
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (PoolMember m : members) {
            BigDecimal cut = hasCut(m, held) && shares.signum() > 0
                    ? pot.multiply(new BigDecimal(m.share())).divide(shares, 2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(2);
            cuts.add(cut);
            sum = sum.add(cut);
        }
        int smallest = -1;
        int lastShare = -1;
        for (int i = 0; i < members.size(); i++) {
            if (!hasCut(members.get(i), held)) {
                continue;
            }
            if (new BigDecimal(members.get(i).share()).signum() > 0) {
                lastShare = i;
            }
            BigDecimal cut = cuts.get(i);
            if (cut.signum() != 0 && (smallest < 0 || cut.compareTo(cuts.get(smallest)) <= 0)) {
                smallest = i;
            }
        }
        int takes = smallest >= 0 ? smallest : lastShare;
        if (takes >= 0 && shares.signum() > 0) {
            cuts.set(takes, cuts.get(takes).add(pot.subtract(sum)));
        }
        List<PoolMember> out = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            out.add(new PoolMember(m.person(), m.memberPool(), m.name(), m.share(), m.retired(), m.from(), m.to(),
                    m.current(), m.current() ? cuts.get(i).toPlainString() : "", m.cuts()));
        }
        return out;
    }

    /** Whether the member takes a cut: within their membership dates, and a pool or a person neither retired nor held. */
    private static boolean hasCut(PoolMember m, Set<Long> held) {
        return m.current() && (m.memberPool() != null || !(m.retired() || held.contains(m.person())));
    }

    /** Whether the sheet has a pool of this name, one added or a crew. */
    public boolean has(String sheet, String name) {
        return hasAdded(sheet, name) || isCrew(sheet, name);
    }

    /** Whether the sheet has a pool of this name that was added as a pool (not a crew). */
    public boolean hasAdded(String sheet, String name) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet = ? AND name = ?", Integer.class,
                sheet, name);
        return found != null && found > 0;
    }

    /** Whether the pool of this name is a crew, its members the crew's. */
    public boolean isCrew(String sheet, String name) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet = ? AND name = ?", Integer.class,
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
        jdbc.update("INSERT INTO pool (sheet, name, position, added_on) VALUES (?, ?,"
                + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM pool p WHERE p.sheet = ?), ?)", sheet, name, sheet,
                Date.valueOf(clock.today(sheet)));
    }

    public void addMember(String sheet, String pool, long person, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (sheet, pool, person_id, share, from_date, to_date, position, added_on)"
                + " VALUES (?, ?, ?, ?, ?, ?, " + NEXT_MEMBER + ", ?)", sheet, pool, person, share, sqlDate(from),
                sqlDate(to), sheet, sheet, Date.valueOf(clock.today(sheet)));
    }

    public void addSubPool(String sheet, String pool, String member, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_sub (sheet, pool, member, share, from_date, to_date, position, added_on)"
                + " VALUES (?, ?, ?, ?, ?, ?, " + NEXT_MEMBER + ", ?)", sheet, pool, member, share, sqlDate(from),
                sqlDate(to), sheet, sheet, Date.valueOf(clock.today(sheet)));
    }

    private static Date sqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    /** The next position among all pools' members, people and pools. */
    private static final String NEXT_MEMBER = "(SELECT COALESCE(MAX(x.position), 0) + 1 FROM (SELECT position FROM"
            + " pool_member WHERE sheet = ? UNION ALL SELECT position FROM pool_sub WHERE sheet = ?) x)";
}
