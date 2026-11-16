package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.officefloor.hq.app.asof.AsOfView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private static final String COLUMNS = "SELECT sheet_id, grade, pct, from_date, to_date, version, claims_after FROM rate ";

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        LocalDate to = rs.getObject("to_date", LocalDate.class);
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"),
                rs.getBigDecimal("pct").setScale(2).toPlainString(), from == null ? null : from.toString(),
                to == null ? null : to.toString(), 0, rs.getInt("version"), rs.getObject("claims_after", Long.class), null);
    };

    /** How many levels up the parent chain a sheet without a rate of its own looks. */
    public static final int LEVELS = 3;

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's own rates (level 0), by grade, then by from-date (none first), then those of its
     *  parent, its parent's parent, up to {@link #LEVELS} up, then its clone source's chain (see
     *  {@link #ancestors}), each at its level. A grade without a rate
     *  is not listed. While the sheet is shown as of a date, its ancestors' rates are as they stood then. */
    public List<Rate> list(String sheetId) {
        List<Rate> all = new ArrayList<>(own(sheetId));
        List<String> chain = ancestors(sheetId);
        LocalDate asOf = AsOfView.schema() == null ? null
                : jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", LocalDate.class, sheetId)
                        .stream().filter((d) -> d != null).findFirst().orElse(null);
        for (int i = 0; i < chain.size(); i++) {
            int level = i + 1;
            (asOf == null ? own(chain.get(i)) : ownAsOf(chain.get(i), asOf)).forEach((r) -> all.add(r.at(level)));
        }
        return all;
    }

    private List<Rate> own(String sheetId) {
        return withPriors(jdbc.query(COLUMNS + "WHERE sheet_id = ? ORDER BY grade, from_key", ROW, sheetId));
    }

    /** Each forward-only rate given the version it replaced (and that one its own, while forward-only). */
    private List<Rate> withPriors(List<Rate> rates) {
        List<Rate> all = new ArrayList<>();
        for (Rate r : rates) {
            if (r.claimsAfter() == null) {
                all.add(r);
                continue;
            }
            List<Rate> older = jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version, claims_after "
                    + "FROM rate_version WHERE sheet_id = ? AND grade = ? AND from_key = COALESCE(CAST(? AS DATE), "
                    + "DATE '0001-01-01') AND version < ? ORDER BY version DESC", ROW,
                    r.sheetId(), r.grade(), r.from() == null ? null : LocalDate.parse(r.from()), r.version());
            all.add(chain(r, older, 0));
        }
        return all;
    }

    private static Rate chain(Rate rate, List<Rate> older, int next) {
        if (rate.claimsAfter() == null || next >= older.size()) return rate;
        return rate.withPrior(chain(older.get(next), older, next + 1));
    }

    /** The sheets whose rates a grade without one of its own goes by, nearest first: the sheet's parent,
     *  its parent's parent, ... up to {@link #LEVELS} up, then, when the sheet was cloned, the sheet it was
     *  cloned from and that one's own chain (its parents, then its source, and so on); each sheet once. */
    public List<String> ancestors(String sheetId) {
        return chain(sheetId, this::parent, this::source);
    }

    /** The chain {@link #ancestors} walks, with the parent and source of a sheet looked up by the given. */
    public static List<String> chain(String sheetId, UnaryOperator<String> parent, UnaryOperator<String> source) {
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>(Set.of(sheetId));
        for (String at = sheetId; at != null; ) {
            String up = at;
            for (int i = 0; i < LEVELS; i++) {
                up = parent.apply(up);
                if (up == null || !seen.add(up)) break;
                chain.add(up);
            }
            at = source.apply(at);
            if (at != null && seen.add(at)) chain.add(at);
            else at = null;
        }
        return chain;
    }

    /** The sheet the sheet was cloned from; null when it was not cloned. */
    public String source(String sheetId) {
        return jdbc.queryForList("SELECT source_id FROM sheet_source WHERE sheet_id = ?", String.class, sheetId)
                .stream().findFirst().orElse(null);
    }

    /** The sheet's parent sheet id; null when it has none. */
    public String parent(String sheetId) {
        return jdbc.queryForList("SELECT val FROM sheet_setting WHERE sheet_id = ? AND setting = 'parent'",
                String.class, sheetId).stream().filter((v) -> v != null && !v.isEmpty()).findFirst().orElse(null);
    }

    /** The grade's rate for the sheet that applies to the claim (by its id) on the date: the sheet's own
     *  with the latest from-date on or before it that applies to the claim, unless there is none or it
     *  ended (its to-date) before the date, when its parent's is looked up the same way, and so on up to
     *  {@link #LEVELS} up, then those of the sheet it was cloned from (see {@link #ancestors}); null when
     *  none has one. */
    public Rate find(String sheetId, String grade, LocalDate on, long claimId) {
        return inForce(list(sheetId), grade, on.toString(), claimId);
    }

    /** The sheet's rates as they stood at the end of the date: each as the last rate set while the
     *  sheet's today was on or before the date left it, by grade, then by from-date (none first). */
    public List<Rate> listAsOf(String sheetId, LocalDate asOf) {
        List<Rate> all = new ArrayList<>(ownAsOf(sheetId, asOf));
        List<String> chain = ancestors(sheetId);
        for (int i = 0; i < chain.size(); i++) {
            int level = i + 1;
            ownAsOf(chain.get(i), asOf).forEach((r) -> all.add(r.at(level)));
        }
        return all;
    }

    private List<Rate> ownAsOf(String sheetId, LocalDate asOf) {
        return withPriors(jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version, claims_after FROM rate_history h WHERE h.sheet_id = ? "
                + "AND h.id = (SELECT MAX(h2.id) FROM rate_history h2 WHERE h2.sheet_id = h.sheet_id AND h2.grade = h.grade "
                + "AND h2.from_key = h.from_key AND (h2.on_date IS NULL OR h2.on_date <= ?)) AND h.withdrawn = FALSE "
                + "ORDER BY grade, from_key",
                ROW, sheetId, asOf));
    }

    /** The grade's rate among {@code rates} in force on the date (yyyy-MM-dd), as {@link #find} finds it:
     *  the nearest level that has one in force then. */
    public static Rate inForce(List<Rate> rates, String grade, String date) {
        return inForce(rates, grade, date, null);
    }

    /** The grade's rate among {@code rates} that applies to the claim (by its id) on the date (yyyy-MM-dd),
     *  as {@link #inForce(List, String, String)} finds it among the versions that apply to the claim
     *  (a forward-only rate saved after the claim was added does not; the one it replaced may). */
    public static Rate inForce(List<Rate> rates, String grade, String date, long claimId) {
        return inForce(rates, grade, date, Long.valueOf(claimId));
    }

    private static Rate inForce(List<Rate> rates, String grade, String date, Long claimId) {
        int top = rates.stream().mapToInt(Rate::level).max().orElse(0);
        for (int level = 0; level <= top; level++) {
            Rate found = null;
            for (Rate listed : rates) {
                Rate r = claimId == null ? listed : listed.forClaim(claimId);
                if (r == null || r.level() != level || !r.grade().equals(grade) || (r.from() != null && r.from().compareTo(date) > 0)) continue;
                if (found == null || (found.from() == null ? "" : found.from()).compareTo(r.from() == null ? "" : r.from()) < 0) found = r;
            }
            if (found != null && !found.endedBefore(date)) return found;
        }
        return null;
    }

    /** Set the grade's rate from the from-date (null: from the beginning of time) through the to-date
     *  (null: no end). When the grade already has a rate from that from-date, the one it replaces is kept
     *  as an older version and the new one is the next version (the one in force). A from-date on or after
     *  the sheet's today makes it forward-only: it applies only to claims added after now. */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, String user) {
        String match = "sheet_id = ? AND grade = ? AND from_key = COALESCE(CAST(? AS DATE), DATE '0001-01-01')";
        LocalDate today = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", LocalDate.class, sheetId)
                .stream().filter((d) -> d != null).findFirst().orElse(LocalDate.now());
        Long claimsAfter = from == null || from.isBefore(today) ? null
                : jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM claim", Long.class);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, version, pct, from_date, to_date, set_by, set_at, claims_after) "
                + "SELECT sheet_id, grade, version, pct, from_date, to_date, set_by, set_at, claims_after FROM rate WHERE " + match,
                sheetId, grade, from);
        int version = 1 + jdbc.queryForList("SELECT version FROM rate WHERE " + match, Integer.class, sheetId, grade, from)
                .stream().findFirst().orElse(0);
        jdbc.update("DELETE FROM rate WHERE " + match, sheetId, grade, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, set_by, set_at, version, claims_after) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)", sheetId, grade, pct, from, to, user, version, claimsAfter);
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, on_date, version, claims_after) "
                + "VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?)",
                sheetId, grade, pct, from, to, sheetId, version, claimsAfter);
        return new Rate(sheetId, grade, pct.toPlainString(), from == null ? null : from.toString(),
                to == null ? null : to.toString(), 0, version, claimsAfter, null);
    }

    /** Whether the sheet has a rate of its own for the grade from the from-date (null: the beginning of time). */
    public boolean has(String sheetId, String grade, LocalDate from) {
        return !jdbc.queryForList("SELECT version FROM rate WHERE sheet_id = ? AND grade = ? AND from_key = "
                + "COALESCE(CAST(? AS DATE), DATE '0001-01-01')", Integer.class, sheetId, grade, from).isEmpty();
    }

    /** Withdraw the newest version of the grade's rate from the from-date (null: the beginning of time): the
     *  version before it, when there is one, is in force again; with none the rate is gone. */
    public void withdraw(String sheetId, String grade, LocalDate from) {
        String match = "sheet_id = ? AND grade = ? AND from_key = COALESCE(CAST(? AS DATE), DATE '0001-01-01')";
        jdbc.update("DELETE FROM rate WHERE " + match, sheetId, grade, from);
        Long older = jdbc.queryForList("SELECT id FROM rate_version WHERE " + match + " ORDER BY version DESC, id DESC",
                Long.class, sheetId, grade, from).stream().findFirst().orElse(null);
        if (older == null) {
            jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, on_date, version, withdrawn) "
                    + "VALUES (?, ?, 0, ?, NULL, (SELECT today FROM sheet WHERE id = ?), 0, TRUE)",
                    sheetId, grade, from, sheetId);
            return;
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, set_by, set_at, version, claims_after) "
                + "SELECT sheet_id, grade, pct, from_date, to_date, set_by, set_at, version, claims_after "
                + "FROM rate_version WHERE id = ?", older);
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, on_date, version, claims_after) "
                + "SELECT sheet_id, grade, pct, from_date, to_date, (SELECT today FROM sheet WHERE id = ?), version, "
                + "claims_after FROM rate_version WHERE id = ?", sheetId, older);
        jdbc.update("DELETE FROM rate_version WHERE id = ?", older);
    }
}
