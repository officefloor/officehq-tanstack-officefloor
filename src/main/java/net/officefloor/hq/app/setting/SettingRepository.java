package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settings: the dated ones (see SettingHistory) one value per key and from-date, the policy date and
 *  the period numbering from (not dated) on the sheet's row. */
@Repository
public class SettingRepository {

    /** The floor of a sheet before any is in force. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms of a sheet before any are in force. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent of a sheet before any is in force. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings in force on its today. */
    public Settings of(String sheetId) {
        return of(sheetId, history(sheetId), today(sheetId));
    }

    /** The sheet's settings as they stood at the end of the day, in force on it: its dated settings then (see
     *  historyAsOf); the ones that are not dated as they are now. */
    public Settings asOf(String sheetId, LocalDate end) {
        return of(sheetId, historyAsOf(sheetId, end), end);
    }

    private Settings of(String sheetId, SettingHistory history, LocalDate today) {
        Map<String, Integer> gradeTerms = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            Integer days = history.gradeTermsOn(grade, today);
            if (days != null) {
                gradeTerms.put(grade, days);
            }
        }
        Map<String, Integer> gradeGrace = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            Integer days = history.graceOn(grade, today);
            if (days != null) {
                gradeGrace.put(grade, days);
            }
        }
        Map<String, BigDecimal> gradeProofThreshold = new LinkedHashMap<>();
        for (String grade : ValidateSettings.GRADES) {
            BigDecimal amount = history.gradeProofThresholdOn(grade, today);
            if (amount != null) {
                gradeProofThreshold.put(grade, amount);
            }
        }
        return new Settings(sheetId, history.clerkLimitOn(today), history.floorOn(today), history.termsOn(today),
                history.latePctOn(today), policyDate(sheetId), periodNumberingFrom(sheetId), gradeTerms,
                history.capOn(today), history.proofThresholdOn(today), history.noteThresholdOn(today), history, permissions(sheetId), graceDays(sheetId),
                entryDate(sheetId), gradeGrace, gradeProofThreshold, pot(sheetId), parent(sheetId),
                fiscalStartMonth(sheetId), alertThreshold(sheetId), history.roundingOn(today),
                history.reviewThresholdOn(today), escalateAfter(sheetId), reviewSla(sheetId), proofKinds(sheetId));
    }

    /** Every value of the sheet's dated settings, by key and from-date. */
    public SettingHistory history(String sheetId) {
        Map<String, NavigableMap<LocalDate, String>> values = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, val FROM setting_value WHERE sheet_id = ?", rs -> {
            values.computeIfAbsent(rs.getString(1), k -> new TreeMap<>())
                    .put(rs.getObject(2, LocalDate.class), rs.getString(3));
        }, sheetId);
        return new SettingHistory(values);
    }

    /** Record the dated setting's value (null: blank) from the from-date, replacing any it had from that date. */
    public void setDated(String sheetId, String key, String value, LocalDate from) {
        jdbc.update("DELETE FROM setting_value WHERE sheet_id = ? AND setting_key = ? AND from_date = ?",
                sheetId, key, from);
        jdbc.update("INSERT INTO setting_value (sheet_id, setting_key, from_date, val) VALUES (?, ?, ?, ?)",
                sheetId, key, from, value);
        jdbc.update("INSERT INTO setting_log (sheet_id, setting_key, from_date, val, made_on) VALUES (?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", sheetId, key, from, value, sheetId);
    }

    /** The sheet's dated settings as they stood at the end of the day: each value recorded from a from-date replaces
     *  the one recorded before it for the key from that same date. */
    public SettingHistory historyAsOf(String sheetId, LocalDate end) {
        Map<String, NavigableMap<LocalDate, String>> values = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, val FROM setting_log WHERE sheet_id = ? "
                + "AND (made_on IS NULL OR made_on <= ?) ORDER BY id", rs -> {
                    values.computeIfAbsent(rs.getString(1), k -> new TreeMap<>())
                            .put(rs.getObject(2, LocalDate.class), rs.getString(3));
                }, sheetId, end);
        return new SettingHistory(values);
    }

    /** The sheet's clerk limit in force on its today: two decimals, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        return history(sheetId).clerkLimitOn(today(sheetId));
    }

    /** The sheet's cap in force on its today: two decimals, or null when there is none; a person's due-total is at
     *  most it. */
    public BigDecimal cap(String sheetId) {
        return history(sheetId).capOn(today(sheetId));
    }

    /** The sheet's proof threshold by date (null on a date when there is none then): a claim whose due is above the one
     *  in force on its date cannot be settled or approved while it has no proofs. */
    public DatedAmount proofThreshold(String sheetId) {
        return history(sheetId)::proofThresholdOn;
    }

    /** The proof threshold a claim of the grade dated on the date has: the grade's own in force then when it has one,
     *  else the sheet's in force then; null when there is none. */
    public BigDecimal proofThresholdFor(String sheetId, String grade, LocalDate date) {
        return history(sheetId).proofThresholdFor(grade, date);
    }

    /** The sheet's note threshold by date (null on a date when there is none then): a claim whose due is above the one
     *  in force on its date cannot be opened while its note is empty. */
    public DatedAmount noteThreshold(String sheetId) {
        return history(sheetId)::noteThresholdOn;
    }

    /** The sheet's review threshold by date (null on a date when there is none then): a claim whose due is above the
     *  one in force on its date goes to REVIEW when opened. */
    public DatedAmount reviewThreshold(String sheetId) {
        return history(sheetId)::reviewThresholdOn;
    }

    /** The sheet's floor by date: a claim's live due is at least the floor in force on its date. */
    public DatedAmount floor(String sheetId) {
        return history(sheetId)::floorOn;
    }

    /** The sheet's late percent by date: an overdue claim gains, for each full 30 days, the one in force on that
     *  boundary date, of its figure. */
    public DatedAmount latePct(String sheetId) {
        return history(sheetId)::latePctOn;
    }

    /** The terms in days a claim of the grade dated on the date gets: the grade's own in force then, else the
     *  sheet's in force then. */
    public int termsFor(String sheetId, String grade, LocalDate date) {
        return history(sheetId).termsFor(grade, date);
    }

    /** The sheet's permission matrix: the roles each action is open to. */
    public Permissions permissions(String sheetId) {
        Map<String, String> roles = new HashMap<>();
        jdbc.query("SELECT action, roles FROM sheet_permission WHERE sheet_id = ?",
                rs -> {
                    roles.put(rs.getString(1), rs.getString(2));
                }, sheetId);
        return new Permissions(roles);
    }

    /** Set the roles the action is open to (blank: the standing rule). */
    public void setPermission(String sheetId, String action, String roles) {
        jdbc.update("DELETE FROM sheet_permission WHERE sheet_id = ? AND action = ?", sheetId, action);
        if (!roles.isEmpty()) {
            jdbc.update("INSERT INTO sheet_permission (sheet_id, action, roles) VALUES (?, ?, ?)", sheetId, action, roles);
        }
    }

    /** The sheet's policy date: null when blank (the sheet's today is the policy date then). */
    public LocalDate policyDate(String sheetId) {
        return jdbc.query("SELECT policy_date FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }

    public void setPolicyDate(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", date, sheetId);
    }

    /** The sheet's period numbering from: null when blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        return jdbc.query("SELECT period_numbering_from FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", date, sheetId);
    }

    /** The sheet's entry date: null when blank (off: a new claim given no date is dated the sheet's today). */
    public LocalDate entryDate(String sheetId) {
        return jdbc.query("SELECT entry_date FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }

    public void setEntryDate(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET entry_date = ? WHERE id = ?", date, sheetId);
    }

    /** The sheet's pot: null when blank (none). */
    public BigDecimal pot(String sheetId) {
        return jdbc.query("SELECT pot FROM sheet WHERE id = ?",
                rs -> rs.next() && rs.getBigDecimal(1) != null ? rs.getBigDecimal(1).setScale(2) : null, sheetId);
    }

    /** The sheet's pot as it stood at the end of the day: the last one set while its today was on or before it;
     *  null when none was (or it was blank). */
    public BigDecimal potAsOf(String sheetId, LocalDate end) {
        return jdbc.query("SELECT val FROM pot_log WHERE sheet_id = ? AND (made_on IS NULL OR made_on <= ?) "
                + "ORDER BY id DESC LIMIT 1",
                rs -> rs.next() && rs.getBigDecimal(1) != null ? rs.getBigDecimal(1).setScale(2) : null, sheetId, end);
    }

    public void setPot(String sheetId, BigDecimal pot) {
        jdbc.update("UPDATE sheet SET pot = ? WHERE id = ?", pot, sheetId);
        jdbc.update("INSERT INTO pot_log (sheet_id, val, made_on) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, pot, sheetId);
    }

    /** The sheet's escalation gap in days: null when blank (off). */
    public Integer escalateAfter(String sheetId) {
        return jdbc.query("SELECT escalate_after FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, Integer.class) : null, sheetId);
    }

    public void setEscalateAfter(String sheetId, Integer days) {
        jdbc.update("UPDATE sheet SET escalate_after = ? WHERE id = ?", days, sheetId);
    }

    /** The sheet's review SLA in days: null when blank (none). */
    public Integer reviewSla(String sheetId) {
        return jdbc.query("SELECT review_sla FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, Integer.class) : null, sheetId);
    }

    public void setReviewSla(String sheetId, Integer days) {
        jdbc.update("UPDATE sheet SET review_sla = ? WHERE id = ?", days, sheetId);
    }

    /** The sheet's proof kinds, comma separated: null when blank (none). While set, a claim above the proof threshold
     *  needs at least one proof of each listed kind before it can be settled or approved. */
    public String proofKinds(String sheetId) {
        return jdbc.query("SELECT proof_kinds FROM sheet WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, sheetId);
    }

    public void setProofKinds(String sheetId, String kinds) {
        jdbc.update("UPDATE sheet SET proof_kinds = ? WHERE id = ?", kinds, sheetId);
    }

    /** The sheet's alert threshold: null when blank (off). */
    public BigDecimal alertThreshold(String sheetId) {
        return jdbc.query("SELECT alert_threshold FROM sheet WHERE id = ?",
                rs -> rs.next() && rs.getBigDecimal(1) != null ? rs.getBigDecimal(1).setScale(2) : null, sheetId);
    }

    public void setAlertThreshold(String sheetId, BigDecimal threshold) {
        jdbc.update("UPDATE sheet SET alert_threshold = ? WHERE id = ?", threshold, sheetId);
    }

    /** The sheet's parent sheet: null when there is none. */
    public String parent(String sheetId) {
        return jdbc.query("SELECT parent FROM sheet WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, sheetId);
    }

    /** The sheets naming this sheet as their parent, by id. */
    public List<String> children(String sheetId) {
        return jdbc.queryForList("SELECT id FROM sheet WHERE parent = ? ORDER BY id", String.class, sheetId);
    }

    public void setParent(String sheetId, String parent) {
        jdbc.update("UPDATE sheet SET parent = ? WHERE id = ?", parent, sheetId);
    }

    /** The sheet's fiscal year start month, 1 to 12 (1 unless set). */
    public int fiscalStartMonth(String sheetId) {
        Integer month = jdbc.query("SELECT fiscal_start_month FROM sheet WHERE id = ?",
                rs -> rs.next() ? (Integer) rs.getObject(1, Integer.class) : null, sheetId);
        return month != null ? month : 1;
    }

    public void setFiscalStartMonth(String sheetId, int month) {
        jdbc.update("UPDATE sheet SET fiscal_start_month = ? WHERE id = ?", month, sheetId);
    }

    /** The sheet's grace days: null when blank (off). */
    public Integer graceDays(String sheetId) {
        return jdbc.query("SELECT grace_days FROM sheet WHERE id = ?",
                rs -> rs.next() ? (Integer) rs.getObject(1, Integer.class) : null, sheetId);
    }

    public void setGraceDays(String sheetId, Integer days) {
        jdbc.update("UPDATE sheet SET grace_days = ? WHERE id = ?", days, sheetId);
    }

    /** The sheet's today: dated settings are shown, and saved by default, as of it. */
    public LocalDate today(String sheetId) {
        return jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }
}
