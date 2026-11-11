package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's settings; read as of a date, as they stood then. */
public class RespondWithSettings {
    public void respond(@Val Sheet sheet, SettingsRepository repository, SheetRepository sheets,
            net.officefloor.hq.app.grade.GradeRepository grades,
            HttpServletRequest request, ObjectResponse<SettingsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        response.send(SettingsResponse.from(asOf == null ? repository.find(sheet.id()) : repository.findAsOf(sheet.id(), asOf),
                sheets.parent(sheet.id()), grades.grades(sheet.id())));
    }
}
