package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
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
     *  member (a retirement date set, whatever the date) counts their share as given toward the total
     *  (but has no part of the cuts, see {@link #cuts}); a member whose
     *  from- and to-dates do not include the sheet's today is left out (no share toward the total, no
     *  cut). A member may be
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
        Set<String> stored = new HashSet<>(members.keySet());
        // Each crew is a pool too, after the pools: its lead then its members, at equal shares of 100.00.
        crewPools(sheetId, members, totals);
        Map<String, String> parents = new HashMap<>();
        jdbc.query("SELECT m.pool, m.person_id, COALESCE(p.name, m.member_pool), m.share, p.retired_from IS NOT NULL, "
                + "m.member_pool, m.from_date, m.to_date, "
                + "(m.from_date IS NULL OR s.today IS NULL OR m.from_date <= s.today) "
                + "AND (m.to_date IS NULL OR s.today IS NULL OR m.to_date >= s.today) "
                + "FROM pool_member m LEFT JOIN person p ON p.id = m.person_id "
                + "LEFT JOIN sheet s ON s.id = m.sheet_id "
                + "WHERE m.sheet_id = ? ORDER BY m.position", rs -> {
                    String pool = rs.getString(1);
                    List<PoolMemberResponse> list = members.get(pool);
                    if (list == null) return;
                    String memberPool = rs.getString(6);
                    Long personId = memberPool == null ? rs.getLong(2) : null;
                    BigDecimal share = rs.getBigDecimal(4).setScale(2);
                    boolean retired = memberPool == null && rs.getBoolean(5);
                    Date from = rs.getDate(7);
                    Date to = rs.getDate(8);
                    boolean current = rs.getBoolean(9);
                    list.add(new PoolMemberResponse(personId, rs.getString(3), share.toPlainString(), retired, null,
                            memberPool, from == null ? null : from.toLocalDate().toString(),
                            to == null ? null : to.toLocalDate().toString(), current));
                    if (memberPool != null) parents.put(memberPool, pool);
                    if (current) totals.merge(pool, share, BigDecimal::add);
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
            pools.add(new PoolResponse(name, shown.getOrDefault(name, list), total.toPlainString(), balanced,
                    !stored.contains(name)));
        });
        return pools;
    }

    /** Each person's cut total: the cuts of the pools' person members added up, by person id (a person
     *  with no cut is not in it). A member pool, or a member with no cut, adds nothing. */
    public Map<Long, BigDecimal> cutTotals(String sheetId) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (PoolResponse pool : onSheet(sheetId)) {
            for (PoolMemberResponse m : pool.members()) {
                if (m.personId() != null && m.cut() != null) totals.merge(m.personId(), new BigDecimal(m.cut()), BigDecimal::add);
            }
        }
        return totals;
    }

    /** Adds each crew of the sheet (in the order added) as a pool of its name: its lead (when still on
     *  the sheet) then its members in the order added, 100.00 split equally among them as patrons' shares
     *  are (each rounded half to even to two decimals, the remainder on the lead, or on the first
     *  member when the lead is gone), with no dates. A crew named as a stored pool is left out. */
    private void crewPools(String sheetId, Map<String, List<PoolMemberResponse>> members,
            Map<String, BigDecimal> totals) {
        Map<String, List<Object[]>> crews = new LinkedHashMap<>();
        jdbc.query("SELECT c.name, p.id, p.name, p.retired_from IS NOT NULL FROM crew c "
                + "LEFT JOIN person p ON p.id = c.lead_id AND p.sheet_id = c.sheet_id "
                + "WHERE c.sheet_id = ? ORDER BY c.position", rs -> {
                    List<Object[]> list = new ArrayList<>();
                    if (rs.getString(3) != null) list.add(new Object[] { rs.getLong(2), rs.getString(3), rs.getBoolean(4) });
                    crews.put(rs.getString(1), list);
                }, sheetId);
        jdbc.query("SELECT m.crew, p.id, p.name, p.retired_from IS NOT NULL FROM crew_member m "
                + "JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.position", rs -> {
                    List<Object[]> list = crews.get(rs.getString(1));
                    if (list != null) list.add(new Object[] { rs.getLong(2), rs.getString(3), rs.getBoolean(4) });
                }, sheetId);
        crews.forEach((name, people) -> {
            if (members.containsKey(name)) return;
            List<PoolMemberResponse> list = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            if (!people.isEmpty()) {
                BigDecimal each = FULL.divide(BigDecimal.valueOf(people.size()), 2, RoundingMode.HALF_EVEN);
                BigDecimal first = FULL.subtract(each.multiply(BigDecimal.valueOf(people.size() - 1)));
                for (int i = 0; i < people.size(); i++) {
                    Object[] person = people.get(i);
                    BigDecimal share = i == 0 ? first : each;
                    list.add(new PoolMemberResponse((Long) person[0], (String) person[1], share.toPlainString(),
                            (Boolean) person[2], null, null, null, null, true));
                    total = total.add(share);
                }
            }
            members.put(name, list);
            totals.put(name, total);
        });
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

    /** The members with their cuts of the pot. A retired member is left out of the cuts and a person
     *  with a hold in force on the sheet's today has none: each shows 0.00, their share spread over the
     *  others in proportion to those others' shares. When every member in their dates is retired, no
     *  member has a cut at all (null); each other member's is the pot times their share divided by
     *  the shares of the members with a cut added up, rounded half to even to two decimals. The
     *  remainder, the pot less the cuts, goes to the member with the smallest non-zero cut, the latest
     *  added on a tie (the last member with a cut when every cut is 0.00), so the cuts total the pot
     *  (when no member has a cut, each is 0.00). A member out of their dates is left out: no cut at all (null). */
    private static List<PoolMemberResponse> cuts(List<PoolMemberResponse> members, BigDecimal pot, Set<Long> held) {
        boolean allRetired = members.stream().anyMatch(PoolMemberResponse::current)
                && members.stream().filter(PoolMemberResponse::current).allMatch(PoolMemberResponse::retired);
        if (allRetired) {
            List<PoolMemberResponse> none = new ArrayList<>(members.size());
            for (PoolMemberResponse m : members) {
                none.add(new PoolMemberResponse(m.personId(), m.name(), m.share(), m.retired(), null, m.pool(),
                        m.from(), m.to(), m.current()));
            }
            return none;
        }
        BigDecimal[] cuts = new BigDecimal[members.size()];
        boolean[] cut = new boolean[members.size()];
        BigDecimal shares = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            cut[i] = m.current() && !m.retired() && (m.personId() == null || !held.contains(m.personId()));
            if (cut[i]) shares = shares.add(new BigDecimal(m.share()));
        }
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int smallest = -1;
        int last = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = !cut[i] || shares.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (cut[i] && shares.signum() != 0) {
                last = i;
                if (cuts[i].signum() != 0 && (smallest < 0 || cuts[i].compareTo(cuts[smallest]) <= 0)) smallest = i;
            }
        }
        int to = smallest >= 0 ? smallest : last;
        if (to >= 0) cuts[to] = cuts[to].add(pot.subtract(sum));
        List<PoolMemberResponse> withCuts = new ArrayList<>(members.size());
        for (int i = 0; i < members.size(); i++) {
            PoolMemberResponse m = members.get(i);
            withCuts.add(new PoolMemberResponse(m.personId(), m.name(), m.share(), m.retired(),
                    m.current() ? cuts[i].toPlainString() : null, m.pool(), m.from(), m.to(), m.current()));
        }
        return withCuts;
    }

    /** The to-date of each period of the sheet whose cuts are shown: one whose to-date is on or before
     *  the sheet's today, or one closed (or soft-closed) with its cuts fixed, whatever its to-date; by
     *  name, by from-date. */
    public Map<String, LocalDate> periodsShown(String sheetId) {
        Map<String, LocalDate> shown = new LinkedHashMap<>();
        jdbc.query("SELECT p.name, p.to_date FROM period p JOIN sheet s ON s.id = p.sheet_id "
                + "WHERE p.sheet_id = ? AND (p.to_date <= s.today OR (p.closed AND EXISTS (SELECT 1 FROM period_pool_cut c "
                + "WHERE c.sheet_id = p.sheet_id AND c.period_name = p.name))) ORDER BY p.from_date, p.to_date, p.name",
                rs -> {
                    shown.put(rs.getString(1), rs.getDate(2).toLocalDate());
                }, sheetId);
        return shown;
    }

    /** Each closed period's cuts fixed when it was closed: by period, then pool, then member key (the
     *  cut null when the member had none). A period still open has none. */
    public Map<String, Map<String, Map<String, String>>> fixedCuts(String sheetId) {
        Map<String, Map<String, Map<String, String>>> fixed = new HashMap<>();
        jdbc.query("SELECT c.period_name, c.pool, c.member_key, c.cut FROM period_pool_cut c "
                + "JOIN period p ON p.sheet_id = c.sheet_id AND p.name = c.period_name "
                + "WHERE c.sheet_id = ? AND p.closed", rs -> {
                    BigDecimal cut = rs.getBigDecimal(4);
                    fixed.computeIfAbsent(rs.getString(1), k -> new HashMap<>())
                            .computeIfAbsent(rs.getString(2), k -> new HashMap<>())
                            .put(rs.getString(3), cut == null ? null : cut.setScale(2).toPlainString());
                }, sheetId);
        return fixed;
    }

    /** Fix every pool member's cut for the period as the sheet stands now, afresh. */
    public void fixCuts(String sheetId, String period) {
        clearCuts(sheetId, period);
        for (PoolResponse p : onSheet(sheetId)) {
            for (PoolMemberResponse m : p.members()) {
                jdbc.update("INSERT INTO period_pool_cut (sheet_id, period_name, pool, member_key, cut) VALUES (?, ?, ?, ?, ?)",
                        sheetId, period, p.name(), m.key(), m.cut() == null ? null : new BigDecimal(m.cut()));
            }
        }
    }

    /** The period's cuts are live again. */
    public void clearCuts(String sheetId, String period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, period);
    }

    /** Whether the sheet has a pool of that name. */
    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the sheet has a crew of that name (a crew is a pool too). */
    public boolean hasCrew(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?",
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

    /** A member of the pool: a person (memberPool null) or another pool (personId null), between from
     *  and to (each null for none). */
    public void addMember(String sheetId, String pool, Long personId, String memberPool, BigDecimal share,
            LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool, person_id, member_pool, share, from_date, to_date, position) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT COALESCE(MAX(position), 0) + 1 FROM pool_member WHERE sheet_id = ?))",
                sheetId, pool, personId, memberPool, share, from == null ? null : Date.valueOf(from),
                to == null ? null : Date.valueOf(to), sheetId);
    }

    /** The name of the sheet's person with that id, or null when the sheet has none. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.query("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                (rs, i) -> rs.getString(1), sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }
}
