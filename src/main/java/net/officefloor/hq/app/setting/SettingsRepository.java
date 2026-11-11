package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The settings store: the dated values in setting_value, the undated ones on the sheet's row. */
@Repository
public class SettingsRepository {

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Settings find(String sheetId) {
        Map<String, NavigableMap<LocalDate, String>> dated = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM setting_value WHERE sheet_id = ?",
                (RowCallbackHandler) (rs) -> dated.computeIfAbsent(rs.getString("setting_key"), (k) -> new TreeMap<>())
                        .put(rs.getDate("from_date").toLocalDate(), rs.getString("setting_value")), sheetId);
        Map<String, String> perms = new HashMap<>();
        jdbc.query("SELECT action, roles FROM setting_perm WHERE sheet_id = ?",
                (RowCallbackHandler) (rs) -> perms.put(rs.getString("action"), rs.getString("roles")), sheetId);
        List<Settings> found = jdbc.query("SELECT today, policy_date, period_numbering_from, grace_days, entry_date, pot, fiscal_start_month, alert_threshold, escalate_after, review_sla, proof_kinds, default_grade, score_mid, score_high, score_log, max_open, pending_sla FROM sheet WHERE id = ?",
                (rs, n) -> new Settings(sheetId, rs.getDate("today").toLocalDate(), dated,
                        rs.getDate("policy_date") == null ? null : rs.getDate("policy_date").toLocalDate(),
                        rs.getDate("period_numbering_from") == null ? null : rs.getDate("period_numbering_from").toLocalDate(), perms,
                        rs.getObject("grace_days", Integer.class),
                        rs.getDate("entry_date") == null ? null : rs.getDate("entry_date").toLocalDate(),
                        rs.getBigDecimal("pot"), rs.getObject("fiscal_start_month", Integer.class),
                        rs.getBigDecimal("alert_threshold"), rs.getObject("escalate_after", Integer.class),
                        rs.getObject("review_sla", Integer.class), rs.getString("proof_kinds"), rs.getString("default_grade"),
                        rs.getObject("score_mid", Integer.class), rs.getObject("score_high", Integer.class), rs.getBoolean("score_log"),
                        rs.getObject("max_open", Integer.class), rs.getObject("pending_sla", Integer.class)),
                sheetId);
        return found.isEmpty() ? new Settings(sheetId, null, dated, null, null, perms, null, null, null, null, null, null, null, null, null, null, null, false, null, null) : found.get(0);
    }

    /** Records a dated setting's value from a date, replacing any value already recorded from it. */
    public void record(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("MERGE INTO setting_value (sheet_id, setting_key, from_date, setting_value) KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)",
                sheetId, key, java.sql.Date.valueOf(from), value);
        history(sheetId, key, from, value);
    }

    /** Keeps a setting's value in its history, with the sheet's today now. */
    private void history(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)"
                + " VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, key, from == null ? null : java.sql.Date.valueOf(from), value, sheetId);
    }

    /**
     * The sheet's settings as they stood at the end of {@code day}: each dated value, the policy date
     * and the pot as last recorded while the sheet's today was on or before it, and that date standing in for
     * the sheet's today. The others are as they are now.
     */
    public Settings findAsOf(String sheetId, LocalDate day) {
        Settings now = find(sheetId);
        Map<String, NavigableMap<LocalDate, String>> dated = new HashMap<>();
        LocalDate[] policy = { null };
        boolean[] policyKept = { false };
        BigDecimal[] pot = { null };
        boolean[] potKept = { false };
        jdbc.query("SELECT setting_key, from_date, setting_value, set_on FROM setting_history WHERE sheet_id = ? ORDER BY id",
                (RowCallbackHandler) (rs) -> {
                    String key = rs.getString("setting_key");
                    String value = rs.getString("setting_value");
                    boolean counts = !rs.getDate("set_on").toLocalDate().isAfter(day);
                    if (POLICY_DATE.equals(key)) {
                        policyKept[0] = true;
                        if (counts) policy[0] = value.isBlank() ? null : LocalDate.parse(value);
                    } else if (POT.equals(key)) {
                        potKept[0] = true;
                        if (counts) pot[0] = value.isBlank() ? null : new BigDecimal(value);
                    } else if (counts) {
                        dated.computeIfAbsent(key, (k) -> new TreeMap<>()).put(rs.getDate("from_date").toLocalDate(), value);
                    }
                }, sheetId);
        return new Settings(sheetId, day, dated, policyKept[0] ? policy[0] : now.policyDate(),
                now.periodNumberingFrom(), now.perms(), now.graceDays(), now.entryDate(), potKept[0] ? pot[0] : now.pot(), now.fiscalStartMonth(),
                now.alertThreshold(), now.escalateAfter(), now.reviewSla(), now.proofKinds(), now.defaultGrade(),
                now.scoreMid(), now.scoreHigh(), now.scoreLog(), now.maxOpen(), now.pendingSla());
    }

    private static final String POLICY_DATE = "policy-date";

    private static final String POT = "pot";

    /** Records the roles ('' for the standing rule) that may take an action. */
    public void recordPerm(String sheetId, String action, String roles) {
        jdbc.update("MERGE INTO setting_perm (sheet_id, action, roles) KEY (sheet_id, action) VALUES (?, ?, ?)",
                sheetId, action, roles);
    }

    /** Saves the undated settings. */
    public void saveUndated(Settings settings) {
        jdbc.update("UPDATE sheet SET policy_date = ?, period_numbering_from = ?, grace_days = ?, entry_date = ?, pot = ?, fiscal_start_month = ?, alert_threshold = ?, escalate_after = ?, review_sla = ?, proof_kinds = ?, default_grade = ?, score_mid = ?, score_high = ?, score_log = ?, max_open = ?, pending_sla = ? WHERE id = ?",
                settings.policyDate() == null ? null : java.sql.Date.valueOf(settings.policyDate()),
                settings.periodNumberingFrom() == null ? null : java.sql.Date.valueOf(settings.periodNumberingFrom()),
                settings.graceDays(),
                settings.entryDate() == null ? null : java.sql.Date.valueOf(settings.entryDate()),
                settings.pot(), settings.fiscalStartMonth(), settings.alertThreshold(), settings.escalateAfter(), settings.reviewSla(), settings.proofKinds(), settings.defaultGrade(),
                settings.scoreMid(), settings.scoreHigh(), settings.scoreLog(), settings.maxOpen(), settings.pendingSla(), settings.sheetId());
        history(settings.sheetId(), POLICY_DATE, null,
                settings.policyDate() == null ? "" : settings.policyDate().toString());
        history(settings.sheetId(), POT, null, settings.pot() == null ? "" : settings.pot().toPlainString());
    }
}
