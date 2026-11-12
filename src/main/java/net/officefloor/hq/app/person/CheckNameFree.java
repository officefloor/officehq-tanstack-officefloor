package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses an add whose name another person on the sheet, or anyone on another sheet of its tree (see
 *  {@link SettingStore#tree}), already has as a name or alias, or a contact another person on the sheet has. */
public class CheckNameFree {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store,
            SettingStore settings) throws DuplicateNameException, DuplicateContactException {
        PersonRules.refuseTaken(sheetId, request, store, settings);
    }
}
