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
}
