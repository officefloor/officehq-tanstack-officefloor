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
     *  member (a retirement date set, whatever the date) counts their share toward the total; a member whose dates do not
     *  include the sheet's today is left out of it entirely (and has no cut). A pool that is a member of none
     *  has the sheet's pot; a pool that is a member of another has its cut there as its pot. While a pool has a pot,
     *  each member of it, if balanced, has a cut of it (see {@link #cuts}); otherwise no cut (""). */
    public List<PoolResponse> list(String sheetId) {
        return list(sheetId, null);
    }

    /** The sheet's pools as {@link #list(String)}, with memberships' dates, holds and the pot judged on the date standing for
     *  the sheet's today (null: the sheet's today itself), as the as-of view shows them: a hold set after the date is left out. */
    public List<PoolResponse> list(String sheetId, java.time.LocalDate on) {
        List<PoolResponse> pools = compute(sheetId, on, false);
        java.sql.Date day = on == null ? null : java.sql.Date.valueOf(on);
        Map<String, Map<String, String>> frozen = frozenCuts(sheetId, day);
        Map<String, Map<String, String>> history = new LinkedHashMap<>();
        jdbc.query("SELECT p.name, p.to_date, p.to_date <= COALESCE(CAST(? AS DATE), s.today) FROM period p"
                + " JOIN sheet s ON s.id = p.sheet_id WHERE p.sheet_id = ? ORDER BY p.from_date, p.name", rs -> {
                    String period = rs.getString(1);
                    if (frozen.containsKey(period)) {
                        history.put(period, frozen.get(period));
                        return;
                    }
                    if (!rs.getBoolean(3)) {
                        return;
                    }
                    Map<String, String> cuts = new HashMap<>();
                    for (PoolResponse then : compute(sheetId, rs.getDate(2).toLocalDate(), true)) {
                        for (PoolResponse.Member m : then.members()) {
                            cuts.put(memberKey(then.name(), m), m.cut());
                        }
                    }
                    history.put(period, cuts);
                }, day, sheetId);
        if (history.isEmpty()) {
            return pools;
        }
        List<PoolResponse> withHistory = new ArrayList<>();
        for (PoolResponse pool : pools) {
            List<PoolResponse.Member> members = new ArrayList<>();
            for (PoolResponse.Member m : pool.members()) {
                Map<String, String> periodCuts = new LinkedHashMap<>();
                history.forEach((period, cuts) -> periodCuts.put(period, cuts.getOrDefault(memberKey(pool.name(), m), "")));
                members.add(new PoolResponse.Member(m.personId(), m.memberPool(), m.name(), m.share(), m.retired(),
                        m.current(), m.cut(), periodCuts));
            }
            withHistory.add(new PoolResponse(pool.name(), pool.totalShare(), pool.balanced(), members, pool.crew()));
        }
        return withHistory;
    }

    /** Every pool member's cut as it stands now (see {@link #list(String)}), to be fixed by a close or soft-close of a
     *  period: one "<pool>\t<member key>\t<cut>" line per member. */
    public String liveCuts(String sheetId) {
        StringBuilder lines = new StringBuilder();
        for (PoolResponse pool : compute(sheetId, null, false)) {
            for (PoolResponse.Member m : pool.members()) {
                String key = memberKey(pool.name(), m);
                lines.append(lines.isEmpty() ? "" : "\n").append(pool.name()).append('\t')
                        .append(key.substring(key.indexOf('\u0000') + 1)).append('\t').append(m.cut());
            }
        }
        return lines.toString();
    }

    /** The cuts fixed for each period that was closed or soft-closed at the end of the date (null: now), as its latest
     *  close fixed them, by member key; a period open then (or closed before cuts were fixed) is left out. */
    private Map<String, Map<String, String>> frozenCuts(String sheetId, java.sql.Date day) {
        Map<String, String> latest = new HashMap<>();
        jdbc.query("SELECT name, closed OR soft_closed, pool_cuts FROM period_history WHERE sheet_id = ?"
                + (day == null ? "" : " AND (set_on IS NULL OR set_on <= ?)") + " ORDER BY seq",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> latest.put(rs.getString(1),
                        rs.getBoolean(2) ? rs.getString(3) : null),
                day == null ? new Object[] {sheetId} : new Object[] {sheetId, day});
        Map<String, Map<String, String>> frozen = new HashMap<>();
        latest.forEach((period, lines) -> {
            if (lines == null) {
                return;
            }
            Map<String, String> cuts = new HashMap<>();
            for (String line : lines.split("\n")) {
                String[] parts = line.split("\t", -1);
                if (parts.length == 3) {
                    cuts.put(parts[0] + "\u0000" + parts[1], parts[2]);
                }
            }
            frozen.put(period, cuts);
        });
        return frozen;
    }

    /** The key a member is matched by across dates: its pool and the person's id or the member pool's name. */
    private static String memberKey(String pool, PoolResponse.Member m) {
        return pool + "\u0000" + (m.personId() != null ? "p" + m.personId() : "m" + m.memberPool());
    }

    /** The sheet's pools judged on the date (null: the sheet's today). With then, the pools, members, crews, holds and
     *  retirements are as they stood at the end of the date: those added later are left out, a retirement is the latest
     *  recorded by then. */
    private List<PoolResponse> compute(String sheetId, java.time.LocalDate on, boolean then) {
        java.sql.Date day = on == null ? null : java.sql.Date.valueOf(on);
        String added = then ? " AND (added_on IS NULL OR added_on <= ?)" : "";
        Map<String, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ?" + added + " ORDER BY seq",
                String.class, then ? new Object[] {sheetId, day} : new Object[] {sheetId})) {
            members.put(name, new ArrayList<>());
        }
        Map<Long, Boolean> retiredThen = then ? retiredOn(sheetId, day) : null;
        jdbc.query("SELECT m.pool_name, m.person_id, p.name, m.share, p.retired_from, m.member_pool,"
                + " (m.from_date IS NULL OR m.from_date <= COALESCE(CAST(? AS DATE), s.today))"
                + " AND (m.to_date IS NULL OR m.to_date >= COALESCE(CAST(? AS DATE), s.today))"
                + " FROM pool_member m"
                + " LEFT JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " JOIN sheet s ON s.id = m.sheet_id"
                + " WHERE m.sheet_id = ?" + (then ? " AND (m.added_on IS NULL OR m.added_on <= ?)" : "")
                + " ORDER BY m.seq", rs -> {
                    List<PoolResponse.Member> list = members.get(rs.getString(1));
                    String sub = rs.getString(6);
                    String share = rs.getBigDecimal(4).setScale(2).toPlainString();
                    if (list == null) {
                        return;
                    }
                    boolean current = rs.getBoolean(7);
                    if (sub != null) {
                        list.add(new PoolResponse.Member(null, sub, sub, share, false, current, ""));
                    } else if (rs.getString(3) != null) {
                        boolean retired = retiredThen == null ? rs.getDate(5) != null
                                : retiredThen.getOrDefault(rs.getLong(2), false);
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), null, rs.getString(3),
                                share, retired, current, ""));
                    }
                }, then ? new Object[] {day, day, sheetId, day} : new Object[] {day, day, sheetId});
        Set<String> crews = crewPools(sheetId, members, then ? day : null, retiredThen);
        BigDecimal pot = day == null ? livePot(sheetId) : potOn(sheetId, day);
        Map<String, String> totals = new HashMap<>();
        Set<String> nested = new HashSet<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : list) {
                if (m.current()) {
                    total = total.add(new BigDecimal(m.share()));
                }
                if (m.memberPool() != null) {
                    nested.add(m.memberPool());
                }
            }
            totals.put(name, total.toPlainString());
        });
        Set<String> held = new HashSet<>(jdbc.queryForList("SELECT DISTINCT CAST(h.person_id AS VARCHAR) FROM person_hold h"
                + " JOIN sheet s ON s.id = h.sheet_id WHERE h.sheet_id = ? AND h.from_date <= COALESCE(CAST(? AS DATE), s.today)"
                + " AND (h.to_date IS NULL OR h.to_date >= COALESCE(CAST(? AS DATE), s.today))"
                + (day != null ? " AND (h.set_on IS NULL OR h.set_on <= ?)" : ""), String.class,
                day != null ? new Object[] {sheetId, day, day, day} : new Object[] {sheetId, day, day}));
        Map<String, List<PoolResponse.Member>> shown = new HashMap<>(members);
        for (String name : members.keySet()) {
            if (!nested.contains(name)) {
                share(name, pot, members, totals, held, shown, 0);
            }
        }
        List<PoolResponse> pools = new ArrayList<>();
        members.keySet().forEach(name -> pools.add(new PoolResponse(name, totals.get(name),
                new BigDecimal(totals.get(name)).compareTo(FULL) == 0, shown.get(name), crews.contains(name))));
        return pools;
    }

    /** Adds each of the sheet's crews, in the order added and after the pools, as a pool of its name: the lead and then
     *  the members (in the order added) at equal shares, 100.00 split as patrons' shares are (each rounded half to even to
     *  two decimals, the remainder on the lead), every membership current. Returns the crews' names. */
    private Set<String> crewPools(String sheetId, Map<String, List<PoolResponse.Member>> members, java.sql.Date addedBy,
            Map<Long, Boolean> retiredThen) {
        Set<String> crews = new HashSet<>();
        Map<String, List<PoolResponse.Member>> people = new LinkedHashMap<>();
        Object[] args = addedBy == null ? new Object[] {sheetId} : new Object[] {sheetId, addedBy};
        jdbc.query("SELECT c.name, c.lead_person_id, p.name, p.retired_from FROM crew c"
                + " LEFT JOIN person p ON p.id = c.lead_person_id AND p.sheet_id = c.sheet_id"
                + " WHERE c.sheet_id = ?" + (addedBy == null ? "" : " AND (c.added_on IS NULL OR c.added_on <= ?)")
                + " ORDER BY c.seq", rs -> {
                    List<PoolResponse.Member> list = new ArrayList<>();
                    // A lead no longer on the sheet (moved away) is left out: the crew's members share it all.
                    if (rs.getString(3) != null) {
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), null, rs.getString(3), "",
                                retired(rs.getLong(2), rs.getDate(4), retiredThen), true, ""));
                    }
                    people.put(rs.getString(1), list);
                }, args);
        jdbc.query("SELECT m.crew_name, m.person_id, p.name, p.retired_from FROM crew_member m"
                + " JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " WHERE m.sheet_id = ?" + (addedBy == null ? "" : " AND (m.added_on IS NULL OR m.added_on <= ?)")
                + " ORDER BY m.seq", rs -> {
                    List<PoolResponse.Member> list = people.get(rs.getString(1));
                    if (list != null) {
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), null, rs.getString(3), "",
                                retired(rs.getLong(2), rs.getDate(4), retiredThen), true, ""));
                    }
                }, args);
        people.forEach((name, list) -> {
            if (members.containsKey(name) || list.isEmpty()) {
                return;
            }
            BigDecimal each = FULL.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_EVEN);
            BigDecimal lead = FULL.subtract(each.multiply(BigDecimal.valueOf(list.size() - 1)));
            List<PoolResponse.Member> shared = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PoolResponse.Member m = list.get(i);
                shared.add(new PoolResponse.Member(m.personId(), null, m.name(), (i == 0 ? lead : each).toPlainString(),
                        m.retired(), true, ""));
            }
            members.put(name, shared);
            crews.add(name);
        });
        return crews;
    }

    /** Whether the person counts as retired: a retirement date set now, or (retiredThen given) as it stood then. */
    private static boolean retired(long personId, java.sql.Date retiredFrom, Map<Long, Boolean> retiredThen) {
        return retiredThen == null ? retiredFrom != null : retiredThen.getOrDefault(personId, false);
    }

    /** Whether each person had a retirement date set at the end of the date: the latest recorded by then. */
    private Map<Long, Boolean> retiredOn(String sheetId, java.sql.Date day) {
        Map<Long, Boolean> retired = new HashMap<>();
        jdbc.query("SELECT person_id, retired_from FROM retire_history WHERE sheet_id = ?"
                + " AND (set_on IS NULL OR set_on <= ?) ORDER BY seq",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> retired.put(rs.getLong(1), rs.getDate(2) != null),
                sheetId, day);
        return retired;
    }

    /** The sheet's pot now, at two decimals; null when none. */
    private BigDecimal livePot(String sheetId) {
        List<BigDecimal> pots = jdbc.queryForList("SELECT pot FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        return pots.isEmpty() || pots.get(0) == null ? null : pots.get(0).setScale(2);
    }

    /** The sheet's pot as it stood at the end of the date: the latest set by then (see the as-of view); null when none. */
    private BigDecimal potOn(String sheetId, java.sql.Date day) {
        List<String> set = jdbc.queryForList("SELECT setting_value FROM plain_setting_history WHERE sheet_id = ?"
                + " AND setting_key = ? AND (set_on IS NULL OR set_on <= ?) ORDER BY seq", String.class, sheetId,
                net.officefloor.hq.app.setting.Settings.POT, day);
        String latest = set.isEmpty() ? null : set.get(set.size() - 1);
        return latest == null || latest.isEmpty() ? null : new BigDecimal(latest).setScale(2);
    }

    /** Gives the pool's members their cuts of its pot, when it has one and is balanced, and passes each member pool's
     *  cut down to it as that pool's pot. */
    private static void share(String name, BigDecimal pot, Map<String, List<PoolResponse.Member>> members,
            Map<String, String> totals, Set<String> held, Map<String, List<PoolResponse.Member>> shown, int depth) {
        if (pot == null || depth > MAX_DEPTH || new BigDecimal(totals.get(name)).compareTo(FULL) != 0) {
            return;
        }
        List<PoolResponse.Member> withCuts = cuts(members.get(name), pot, held);
        shown.put(name, withCuts);
        for (PoolResponse.Member m : withCuts) {
            if (m.memberPool() != null && members.containsKey(m.memberPool()) && m.current()) {
                share(m.memberPool(), new BigDecimal(m.cut()), members, totals, held, shown, depth + 1);
            }
        }
    }

    /** The members with their cuts of the pot: the pot times their share divided by the shares of the members with a
     *  cut added up, rounded half to even to two decimals (a retired member, or a person held on the sheet's today,
     *  cuts 0.00, their share spread over the others in proportion to theirs); the remainder, the pot minus the cuts added up, goes to the member with a cut with the smallest
     *  non-zero cut (the latest added on a tie; with no non-zero cut, the last member with a cut), so the cuts total exactly the pot. With no member left with a cut, each cuts 0.00 —
     *  unless every member is retired, when none has a cut ("").
     *  A member whose dates do not include the sheet's today has no cut at all (""). */
    static List<PoolResponse.Member> cuts(List<PoolResponse.Member> members, BigDecimal pot, Set<String> held) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        boolean[] cutting = new boolean[members.size()];
        BigDecimal shares = BigDecimal.ZERO;
        boolean allRetired = true;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            if (m.current() && !m.retired()) {
                allRetired = false;
            }
            cutting[i] = m.current() && !m.retired() && (m.personId() == null || !held.contains(m.personId()));
            if (cutting[i]) {
                shares = shares.add(new BigDecimal(m.share()));
            }
        }
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int smallest = -1;
        int last = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = !m.current() || allRetired ? null : !cutting[i] || shares.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN);
            if (cuts[i] != null) {
                sum = sum.add(cuts[i]);
            }
            if (cutting[i]) {
                last = i;
                if (cuts[i].signum() != 0 && (smallest < 0 || cuts[i].compareTo(cuts[smallest]) <= 0)) {
                    smallest = i;
                }
            }
        }
        int remainder = smallest >= 0 ? smallest : last;
        if (remainder >= 0 && shares.signum() != 0) {
            cuts[remainder] = cuts[remainder].add(pot.subtract(sum));
        }
        List<PoolResponse.Member> withCuts = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolResponse.Member m = members.get(i);
            withCuts.add(new PoolResponse.Member(m.personId(), m.memberPool(), m.name(), m.share(), m.retired(), m.current(),
                    cuts[i] == null ? "" : cuts[i].toPlainString(), m.periodCuts()));
        }
        return withCuts;
    }

    /** Whether the sheet has a pool of the name: one added as a pool, or a crew (every crew is also a pool). */
    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
        return n != null && n > 0 || isCrew(sheetId, name);
    }

    /** Whether the pool of the name is a crew's: its members are the crew's lead and members, never added as a pool's. */
    public boolean isCrew(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
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
        jdbc.update("INSERT INTO pool (sheet_id, name, added_on) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, name, sheetId);
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

    /** Adds the person (memberPool null) or the other pool (personId null) to the pool with the share and the optional from-
     *  and to-dates of the membership. */
    public void addMember(String sheetId, String pool, Long personId, String memberPool, BigDecimal share,
            LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, member_pool, share, from_date, to_date, added_on)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, pool, personId, memberPool,
                share, from, to, sheetId);
    }
}
