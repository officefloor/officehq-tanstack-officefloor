package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, made empty (its today the real date) when the id was never used. */
    public Sheet loadOrCreate(String id, String user) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, id);
        if (known == null || known == 0) {
            try {
                jdbc.update("INSERT INTO sheet (id, created_by, today) VALUES (?, ?, ?)", id, user,
                        Date.valueOf(LocalDate.now()));
            } catch (DuplicateKeyException made) {
                // another user loaded the same new id at the same moment: it is made either way
            }
        }
        return new Sheet(id, today(id).toString(), archived(id));
    }

    /** The sheet's today: the date every date decision and default in the sheet uses. */
    public LocalDate today(String id) {
        Date today = jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", Date.class, id);
        return today.toLocalDate();
    }

    /** Whether a steward has archived the sheet. */
    public boolean archived(String id) {
        Boolean archived = jdbc.queryForObject("SELECT archived FROM sheet WHERE id = ?", Boolean.class, id);
        return Boolean.TRUE.equals(archived);
    }

    /** Archive the sheet; false when there is no such sheet. */
    public boolean archive(String id) {
        return jdbc.update("UPDATE sheet SET archived = TRUE WHERE id = ?", id) > 0;
    }

    /** Set the sheet's today; false when there is no such sheet. */
    public boolean setToday(String id, LocalDate today) {
        return jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", Date.valueOf(today), id) > 0;
    }
}
