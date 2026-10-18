package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
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

    /** A live due raised to the sheet's floor (null, for no rate, stays null). */
    default BigDecimal floored(String sheetId, BigDecimal due) {
        return due == null ? null : due.max(floor(sheetId));
    }
}
