package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheets: a row per id ever loaded. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Create the sheet if its id was never used (its today the real date); an existing sheet is left as it is. */
    public void ensure(String id) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) SELECT ?, COALESCE((SELECT today FROM sheet WHERE id = ?), ?)",
                id, id, LocalDate.now());
    }

    /**
     * The sheet's today. Every decision that depends on the date, and every default date, uses
     * this — never the real clock. A sheet not yet stored is on the real date.
     */
    public LocalDate today(String id) {
        return jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : LocalDate.now(), id);
    }

    public void setToday(String id, LocalDate today) {
        jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", today, id);
    }

    /** Whether the sheet is archived: then every change to it is refused. A sheet not yet stored is not. */
    public boolean isArchived(String id) {
        return jdbc.query("SELECT archived FROM sheet WHERE id = ?", rs -> rs.next() && rs.getBoolean(1), id);
    }

    /** Refuse a change to the sheet while it is archived. */
    public void refuseArchived(String id) throws BadRequestException {
        if (isArchived(id)) {
            throw new BadRequestException("This sheet is archived");
        }
    }

    public void archive(String id) {
        jdbc.update("UPDATE sheet SET archived = TRUE WHERE id = ?", id);
    }
}
