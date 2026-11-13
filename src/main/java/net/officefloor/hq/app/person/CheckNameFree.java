package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses an add whose name another person on the sheet, or anyone on another sheet of its tree (see
 *  {@link SettingStore#tree}), already has as a name or alias, or a contact another person on the sheet has, or
 *  whose grade is retired. */
public class CheckNameFree {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store,
            SettingStore settings, GradeStore grades) throws InvalidSheetException {
        PersonRules.refuseTaken(sheetId, request, store, settings);
        grades.refuseChoice(sheetId.trim(), request.grade(), null, null, null);
    }
}
