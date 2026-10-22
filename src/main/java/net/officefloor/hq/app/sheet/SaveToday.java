package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Set the sheet's today; a change of it releases every claim's editor lock on the sheet. */
public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, SheetRepository repository,
            ClaimRepository claims) {
        String id = sheetId.trim();
        if (!Objects.equals(repository.today(id), today)) {
            claims.unlockAll(id);
        }
        repository.setToday(id, today);
    }
}
