package net.officefloor.hq.app.setting;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each sheet's settings, by key. A key without a stored value is blank. */
@Repository
public class SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The setting's value; empty when it is blank. */
    public Optional<String> find(String sheetId, String key) {
        List<String> found = jdbc.queryForList("SELECT val FROM sheet_setting WHERE sheet_id = ? AND setting = ?",
                String.class, sheetId, key);
        return found.stream().findFirst();
    }

    /** Sets the setting; a blank value clears it. */
    public void save(Setting setting) {
        jdbc.update("DELETE FROM sheet_setting WHERE sheet_id = ? AND setting = ?", setting.sheetId(), setting.key());
        if (!setting.value().isEmpty()) {
            jdbc.update("INSERT INTO sheet_setting (sheet_id, setting, val) VALUES (?, ?, ?)",
                    setting.sheetId(), setting.key(), setting.value());
        }
    }
}
