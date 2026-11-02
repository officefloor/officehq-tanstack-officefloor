package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.holiday.Holidays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A claim's due-by is fixed from the terms in force on the day it was made (the sheet's today then), not on its date.
 * Every claim kept takes the terms in force on the day of its first status log line (its date when it has none): its
 * person's own then, else those of the grade the person had then, else the sheet's then (14 when none is set). A claim
 * whose terms change takes as its due-by the terms-th business day after its date, skipping Saturdays, Sundays and the
 * sheet's holidays as they stand now.
 */
public class V86__claim_due_terms_when_made extends BaseJavaMigration {

    private record Kept(long id, String sheet, long person, LocalDate date, LocalDate made, int terms) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();
        List<Kept> kept = new ArrayList<>();
        try (var ps = con.prepareStatement("SELECT c.id, c.sheet, c.person, c.claimed_on, c.due_terms,"
                + " (SELECT MIN(l.on_date) FROM claim_log l WHERE l.claim = c.id) FROM claim c");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate date = rs.getDate(4).toLocalDate();
                Date made = rs.getDate(6);
                kept.add(new Kept(rs.getLong(1), rs.getString(2), rs.getLong(3), date,
                        made == null ? date : made.toLocalDate(), rs.getInt(5)));
            }
        }
        try (var update = con.prepareStatement("UPDATE claim SET due_terms = ?, due_by = ? WHERE id = ?")) {
            for (Kept claim : kept) {
                int terms = terms(con, claim);
                if (terms == claim.terms()) {
                    continue;
                }
                update.setInt(1, terms);
                update.setDate(2, Date.valueOf(Holidays.businessDaysAfter(claim.date(), terms, holidays(con, claim.sheet()))));
                update.setLong(3, claim.id());
                update.executeUpdate();
            }
        }
    }

    private static int terms(Connection con, Kept claim) throws Exception {
        // The person's own: the dated ones from the latest from-date on or before the day, else those from the
        // beginning of time.
        String own = one(con, "SELECT CAST(terms AS VARCHAR) FROM person_terms WHERE person_id = ? AND from_date <= ?"
                + " ORDER BY from_date DESC LIMIT 1", (ps) -> {
                    ps.setLong(1, claim.person());
                    ps.setDate(2, Date.valueOf(claim.made()));
                }, true);
        if (own == null) {
            own = one(con, "SELECT CAST(terms AS VARCHAR) FROM person WHERE id = ?", (ps) -> ps.setLong(1, claim.person()), false);
        }
        if (own != null && !own.isEmpty() && !own.equals(NONE)) {
            return Integer.parseInt(own);
        }
        String grade = one(con, "SELECT grade FROM person_grade WHERE person_id = ? AND from_key <= ? ORDER BY from_key DESC LIMIT 1",
                (ps) -> {
                    ps.setLong(1, claim.person());
                    ps.setDate(2, Date.valueOf(claim.made()));
                }, false);
        String gradeTerms = grade == null ? null : setting(con, claim, "terms-" + grade);
        if (gradeTerms != null && !gradeTerms.isEmpty()) {
            return Integer.parseInt(gradeTerms);
        }
        String terms = setting(con, claim, "terms");
        return terms != null && !terms.isEmpty() ? Integer.parseInt(terms) : 14;
    }

    /** Stands for a dated row whose terms are none (null), so it is told apart from no row at all. */
    private static final String NONE = "";

    private static String setting(Connection con, Kept claim, String key) throws Exception {
        return one(con, "SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ? AND from_date <= ?"
                + " ORDER BY from_date DESC LIMIT 1", (ps) -> {
                    ps.setString(1, claim.sheet());
                    ps.setString(2, key);
                    ps.setDate(3, Date.valueOf(claim.made()));
                }, false);
    }

    private static Set<LocalDate> holidays(Connection con, String sheet) throws Exception {
        Set<LocalDate> days = new HashSet<>();
        try (var ps = con.prepareStatement("SELECT holiday_on FROM holiday WHERE sheet = ?")) {
            ps.setString(1, sheet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    days.add(rs.getDate(1).toLocalDate());
                }
            }
        }
        return days;
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws Exception;
    }

    /** The first row's value (null: no row); a null value is {@link #NONE} when asked to tell it apart. */
    private static String one(Connection con, String sql, Binder binder, boolean nullAsNone) throws Exception {
        try (var ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String value = rs.getString(1);
                return value == null && nullAsNone ? NONE : value;
            }
        }
    }
}
