package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** The sheet's clerk limit: the highest due a clerk may settle (two decimals). Blank means no limit. */
@Service
public class ClerkLimit {

    public static final String KEY = "clerk-limit";

    private final SettingRepository settings;

    public ClerkLimit(SettingRepository settings) {
        this.settings = settings;
    }

    /** The limit; empty when there is none. */
    public Optional<BigDecimal> of(String sheetId) {
        return settings.find(sheetId, KEY).map(BigDecimal::new);
    }
}
