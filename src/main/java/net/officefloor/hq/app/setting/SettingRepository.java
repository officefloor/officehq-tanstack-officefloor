package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, Long> {

    List<Setting> findBySheetId(String sheetId);

    Optional<Setting> findBySheetIdAndKey(String sheetId, String key);

    /** The sheet's clerk limit, or empty for no limit. */
    default Optional<BigDecimal> clerkLimit(String sheetId) {
        return findBySheetIdAndKey(sheetId, Setting.CLERK_LIMIT).map(Setting::getValue).map(BigDecimal::new);
    }

    /** The sheet's floor: a claim's live due is at least it (1.00 when not set). */
    default BigDecimal floor(String sheetId) {
        return new BigDecimal(findBySheetIdAndKey(sheetId, Setting.FLOOR).map(Setting::getValue)
                .orElse(Setting.DEFAULT_FLOOR));
    }

    /** The sheet's terms in days: a claim is due by its date plus them (14 when not set). */
    default int terms(String sheetId) {
        return Integer.parseInt(findBySheetIdAndKey(sheetId, Setting.TERMS).map(Setting::getValue)
                .orElse(Setting.DEFAULT_TERMS));
    }

    /** The grade's own terms in days, or empty when it has none (the sheet's terms are then in force). */
    default Optional<Integer> gradeTerms(String sheetId, String grade) {
        return grade == null ? Optional.empty()
                : findBySheetIdAndKey(sheetId, Setting.termsOf(grade)).map(Setting::getValue).map(Integer::parseInt);
    }

    /** The terms in force for a claim of the grade: the grade's own, else the sheet's. */
    default int terms(String sheetId, String grade) {
        return gradeTerms(sheetId, grade).orElseGet(() -> terms(sheetId));
    }

    /** The sheet's late percent (0.00 when not set). */
    default BigDecimal latePct(String sheetId) {
        return new BigDecimal(findBySheetIdAndKey(sheetId, Setting.LATE_PCT).map(Setting::getValue)
                .orElse(Setting.DEFAULT_LATE_PCT));
    }

    /** The sheet's policy date, or empty when not set (the sheet's today is then in force). */
    default Optional<LocalDate> policyDate(String sheetId) {
        return findBySheetIdAndKey(sheetId, Setting.POLICY_DATE).map(Setting::getValue).map(LocalDate::parse);
    }

    /** The sheet's period numbering date, or empty when period numbering is off. */
    default Optional<LocalDate> periodNumberingFrom(String sheetId) {
        return findBySheetIdAndKey(sheetId, Setting.PERIOD_NUMBERING_FROM).map(Setting::getValue).map(LocalDate::parse);
    }

    /** How a claim dated on this date rounds its live figures: half up when dated before the policy date
     *  (the sheet's today when none is set), half to even when on or after it. */
    default RoundingMode rounding(String sheetId, LocalDate claimDate, LocalDate today) {
        LocalDate policy = policyDate(sheetId).orElse(today);
        return policy != null && claimDate.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** A live due raised to the sheet's floor (null, for no rate, stays null). */
    default BigDecimal floored(String sheetId, BigDecimal due) {
        return due == null ? null : due.max(floor(sheetId));
    }
}
