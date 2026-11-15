package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddHoliday {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val HolidayRequest request, SheetRepository sheets, HolidayRepository holidays,
            Out<Holiday> holiday) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        holiday.set(holidays.add(id, LocalDate.parse(request.date()), user));
    }
}
