package net.officefloor.hq.app.claim;

import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The tags claims carried on a sheet before they left it, kept so the sheet still counts them as ever used. */
@Repository
public class SheetTags {
    private final JdbcTemplate jdbc;

    public SheetTags(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keeps each of the claims' tags (current or ever carried) as used on the sheet. */
    public void keepOf(String sheet, Collection<Long> claimIds) {
        for (long id : claimIds) {
            List<String> kept = jdbc.queryForList("SELECT tags || ',' || tags_ever FROM claim WHERE sheet = ? AND id = ?",
                    String.class, sheet, id);
            for (String all : kept) {
                for (String tag : all.split(",")) {
                    if (!tag.isEmpty()) {
                        jdbc.update("MERGE INTO sheet_tag (sheet, tag) KEY (sheet, tag) VALUES (?, ?)", sheet, tag);
                    }
                }
            }
        }
    }

    /** The tags kept for the sheet, sorted. */
    public List<String> list(String sheet) {
        return jdbc.queryForList("SELECT tag FROM sheet_tag WHERE sheet = ? ORDER BY tag", String.class, sheet);
    }
}
