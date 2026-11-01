package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A claim keeps the terms its due-by was fixed with, so adding a holiday can work its due-by out again. Every claim kept
 * takes the terms it would be fixed with now: its person's own, else those of the person's grade on its date, else the
 * sheet's on its date (14 when none is set).
 */
public class V49__claim_due_terms extends BaseJavaMigration {

    private record Kept(long id, String sheet, long person, LocalDate date) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();
        List<Kept> kept = new ArrayList<>();
        try (var ps = con.prepareStatement("SELECT id, sheet, person, claimed_on FROM claim"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                kept.add(new Kept(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getDate(4).toLocalDate()));
            }
        }
        try (var alter = con.prepareStatement("ALTER TABLE claim ADD COLUMN due_terms INT")) {
            alter.execute();
        }
        try (var update = con.prepareStatement("UPDATE claim SET due_terms = ? WHERE id = ?")) {
            for (Kept claim : kept) {
                update.setInt(1, terms(con, claim));
                update.setLong(2, claim.id());
                update.executeUpdate();
            }
        }
        try (var alter = con.prepareStatement("ALTER TABLE claim ALTER COLUMN due_terms SET NOT NULL")) {
            alter.execute();
        }
    }

    private static int terms(Connection con, Kept claim) throws Exception {
        String own = one(con, "SELECT CAST(terms AS VARCHAR) FROM person WHERE id = ?", (ps) -> ps.setLong(1, claim.person()));
        if (own != null && !own.isEmpty()) {
            return Integer.parseInt(own);
        }
        String grade = one(con, "SELECT grade FROM person_grade WHERE person_id = ? AND from_key <= ? ORDER BY from_key DESC LIMIT 1",
                (ps) -> {
                    ps.setLong(1, claim.person());
                    ps.setDate(2, Date.valueOf(claim.date()));
                });
        String gradeTerms = grade == null ? null : setting(con, claim, "terms-" + grade);
        if (gradeTerms != null && !gradeTerms.isEmpty()) {
            return Integer.parseInt(gradeTerms);
        }
        String terms = setting(con, claim, "terms");
        return terms != null && !terms.isEmpty() ? Integer.parseInt(terms) : 14;
    }

    private static String setting(Connection con, Kept claim, String key) throws Exception {
        return one(con, "SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ? AND from_date <= ?"
                + " ORDER BY from_date DESC LIMIT 1", (ps) -> {
                    ps.setString(1, claim.sheet());
                    ps.setString(2, key);
                    ps.setDate(3, Date.valueOf(claim.date()));
                });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws Exception;
    }

    private static String one(Connection con, String sql, Binder binder) throws Exception {
        try (var ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
