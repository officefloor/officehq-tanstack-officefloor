package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import net.officefloor.hq.app.settings.SettingsRepository;

/** Terms count business days: each claim's due-by (and each kept in its history) is worked out again as the
 *  terms-th business day after its date, skipping Saturdays, Sundays and the holidays on its sheet now, with the
 *  terms it was fixed with. Those terms are the ones in force for it (the person's own, else their grade's on the
 *  date, else the sheet's on the date) when they give its stored calendar due-by (the date plus the terms, moved
 *  past the holidays); else the calendar days from its date to its due-by. */
public class V53__business_day_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        Map<String, Set<String>> holidays = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getDate(2).toLocalDate().toString());
            }
        }
        Map<Long, Integer> termsOf = new HashMap<>();
        try (PreparedStatement claims = c.prepareStatement("SELECT id, sheet_id, person_id, claim_date, due_by FROM claim");
                ResultSet rs = claims.executeQuery();
                PreparedStatement update = c.prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String sheetId = rs.getString("sheet_id");
                LocalDate date = rs.getDate("claim_date").toLocalDate();
                LocalDate old = rs.getDate("due_by").toLocalDate();
                Set<String> sheetHolidays = holidays.getOrDefault(sheetId, Set.of());
                Integer inForce = termsInForce(c, sheetId, rs.getLong("person_id"), date);
                int terms = inForce != null && inForce >= 0 && pastHolidays(sheetHolidays, date.plusDays(inForce)).equals(old)
                        ? inForce : (int) Math.max(0, ChronoUnit.DAYS.between(date, old));
                termsOf.put(id, terms);
                update.setDate(1, Date.valueOf(SettingsRepository.businessDaysAfter(sheetHolidays, date, terms)));
                update.setLong(2, id);
                update.executeUpdate();
            }
        }
        try (PreparedStatement history = c.prepareStatement("SELECT id, sheet_id, claim_id, claim_date, due_by FROM claim_history");
                ResultSet rs = history.executeQuery();
                PreparedStatement update = c.prepareStatement("UPDATE claim_history SET due_by = ? WHERE id = ?")) {
            while (rs.next()) {
                LocalDate date = rs.getDate("claim_date").toLocalDate();
                Integer terms = termsOf.get(rs.getLong("claim_id"));
                if (terms == null) terms = (int) Math.max(0, ChronoUnit.DAYS.between(date, rs.getDate("due_by").toLocalDate()));
                update.setDate(1, Date.valueOf(SettingsRepository.businessDaysAfter(
                        holidays.getOrDefault(rs.getString("sheet_id"), Set.of()), date, terms)));
                update.setLong(2, rs.getLong("id"));
                update.executeUpdate();
            }
        }
    }

    private static LocalDate pastHolidays(Set<String> holidays, LocalDate date) {
        while (holidays.contains(date.toString())) date = date.plusDays(1);
        return date;
    }

    /** The person's own terms, else their grade's in force on the date, else the sheet's in force on it (14 before any). */
    private static Integer termsInForce(Connection c, String sheetId, long personId, LocalDate date) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT terms FROM person WHERE id = ?")) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getObject(1) != null) return rs.getInt(1);
            }
        }
        String grade = null;
        try (PreparedStatement ps = c.prepareStatement("SELECT grade FROM person_grade WHERE person_id = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1")) {
            ps.setLong(1, personId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) grade = rs.getString(1);
            }
        }
        if (grade != null) {
            String own = setting(c, sheetId, "terms-" + grade, date);
            if (own != null) return Integer.valueOf(own);
        }
        String terms = setting(c, sheetId, "terms", date);
        return terms == null ? 14 : Integer.valueOf(terms);
    }

    private static String setting(Connection c, String sheetId, String key, LocalDate date) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT setting FROM setting_value WHERE sheet_id = ? AND setting_key = ?"
                + " AND from_date <= ? ORDER BY from_date DESC LIMIT 1")) {
            ps.setString(1, sheetId);
            ps.setString(2, key);
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
