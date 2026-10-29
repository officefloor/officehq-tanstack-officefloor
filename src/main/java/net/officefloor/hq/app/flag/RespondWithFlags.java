package net.officefloor.hq.app.flag;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithFlags {
    public void respond(@PathVariable("sheetId") String sheetId, Flags flags, Sheets sheets,
            ObjectResponse<List<FlagResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        response.send(flags.list(sheet).stream().map(f -> FlagResponse.from(f, today)).toList());
    }
}
