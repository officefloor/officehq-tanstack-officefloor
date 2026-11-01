package net.officefloor.hq.app.sheet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheets a steward has archived. An archived sheet stays visible and refuses every change. */
@Repository
public class SheetArchive {

    private final JdbcTemplate jdbc;

    public SheetArchive(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean archived(String sheet) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sheet_archive WHERE sheet = ?", Integer.class, sheet) > 0;
    }

    /** Archives the sheet; false when it already was. */
    public boolean archive(String sheet, String user) {
        return jdbc.update("INSERT INTO sheet_archive (sheet, archived_by) SELECT ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM sheet_archive WHERE sheet = ?)", sheet, user, sheet) > 0;
    }
}
