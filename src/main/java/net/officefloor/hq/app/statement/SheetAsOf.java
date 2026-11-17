package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Setting;
import net.officefloor.hq.app.setting.SettingRepository;

/**
 * A sheet's rates and settings as they stood at the end of a date: every one stored while the sheet's today was on or
 * before it counts (a later one stored for the same grade or key and from-date replacing an earlier, a rate as its next version), every one stored
 * later does not. The date stands in for the sheet's today.
 */
public final class SheetAsOf {

    private final LocalDate date;
    private final List<Rate> rates;
    private final List<Rate> versions;
    private final List<Setting> settings;

    private SheetAsOf(LocalDate date, List<Rate> rates, List<Rate> versions, List<Setting> settings) {
        this.date = date;
        this.rates = rates;
        this.versions = versions;
        this.settings = settings;
    }

    public static SheetAsOf read(String sheetId, LocalDate date, RateRepository rates, SettingRepository settings) {
        Map<String, Rate> byKey = new LinkedHashMap<>();
        List<Rate> versions = new ArrayList<>();
        for (RateRepository.Logged logged : rates.storedBy(sheetId, date)) {
            Rate r = logged.rate();
            String key = r.grade() + "@" + r.from();
            Rate older = byKey.get(key);
            if (logged.withdrawn()) {
                // the newest version goes; the one before it, when there is one, applies again
                if (older != null) {
                    versions.remove(older);
                    Rate back = null;
                    for (Rate v : versions) {
                        if (v.grade().equals(r.grade()) && Objects.equals(v.from(), r.from())
                                && (back == null || v.version() > back.version())) {
                            back = v;
                        }
                    }
                    byKey.remove(key);
                    if (back != null) {
                        byKey.put(key, back);
                    }
                }
                continue;
            }
            Rate version = r.withVersion(older == null ? 1 : older.version() + 1);
            byKey.put(key, version);
            versions.add(version);
        }
        Map<String, Setting> byFrom = new LinkedHashMap<>();
        for (Setting s : settings.storedBy(sheetId, date)) {
            byFrom.put(s.key() + "@" + s.from(), s);
        }
        return new SheetAsOf(date, new ArrayList<>(byKey.values()), versions, new ArrayList<>(byFrom.values()));
    }

    public LocalDate date() {
        return date;
    }

    /** The rates as they stood, by grade, then from-date (one with none first), as RateRepository#list orders them. */
    public List<Rate> rates() {
        List<Rate> sorted = new ArrayList<>(rates);
        sorted.sort(java.util.Comparator.comparing(Rate::grade).thenComparing(Rate::from,
                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        return sorted;
    }

    /** The settings as they stood, by key, then from-date (an undated one first), as SettingRepository#list orders them. */
    public List<Setting> settings() {
        List<Setting> sorted = new ArrayList<>(settings);
        sorted.sort(java.util.Comparator.comparing(Setting::key).thenComparing(Setting::from,
                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        return sorted;
    }

    /** The grade's rate in force on a date (see RateRepository#find); null when none applies. */
    public Rate rate(String grade, LocalDate on) {
        Rate found = null;
        for (Rate r : rates) {
            if (!Objects.equals(r.grade(), grade) || (r.from() != null && r.from().isAfter(on))) {
                continue;
            }
            if (found == null || (r.from() != null && (found.from() == null || r.from().isAfter(found.from())))) {
                found = r;
            }
        }
        return found == null || (found.to() != null && found.to().isBefore(on)) ? null : found;
    }

    /** The older versions of a rate as they stood (see #rates), newest first. */
    public List<Rate> older(Rate rate) {
        List<Rate> older = new ArrayList<>();
        for (Rate r : versions) {
            if (r.grade().equals(rate.grade()) && Objects.equals(r.from(), rate.from()) && r.version() < rate.version()) {
                older.add(0, r);
            }
        }
        return older;
    }

    /** The grade's rate that applies to a claim on a date, added with the added sequence {@code seq} (see
     *  RateRepository#pick), as the rates stood; null when none applies. */
    public Rate rate(String grade, LocalDate on, Long seq) {
        return RateRepository.pick(versions, grade, on, seq);
    }

    /** The setting's value in force on a date (an undated one applies to every date); null when it has none. */
    public String value(String key, LocalDate on) {
        Setting found = null;
        for (Setting s : settings) {
            if (!s.key().equals(key) || (s.from() != null && s.from().isAfter(on))) {
                continue;
            }
            if (found == null || (s.from() != null && (found.from() == null || s.from().isAfter(found.from())))) {
                found = s;
            }
        }
        return found == null || found.value() == null || found.value().isBlank() ? null : found.value();
    }

    /** The floor in force on a date (a claim's date). */
    public BigDecimal floor(LocalDate on) {
        String value = value(SettingRepository.FLOOR, on);
        return value == null ? SettingRepository.DEFAULT_FLOOR : new BigDecimal(value);
    }

    /** The late percent in force on a date (a 30-day boundary). */
    public BigDecimal latePct(LocalDate on) {
        String value = value(SettingRepository.LATE_PCT, on);
        return value == null ? SettingRepository.DEFAULT_LATE_PCT : new BigDecimal(value);
    }

    /** The grace of a grade in force on a date (a claim's date): its days, 0 when it has none. */
    public int grace(String grade, LocalDate on) {
        String value = grade == null ? null : value(SettingRepository.GRACE_PREFIX + grade, on);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /** The cap in force on the date; null when there is none. */
    public BigDecimal cap() {
        String value = value(SettingRepository.CAP, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The policy date (undated); null when blank (the date applies). */
    public LocalDate policyDate() {
        String value = value(SettingRepository.POLICY_DATE, date);
        return value == null ? null : LocalDate.parse(value);
    }
}
