package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Val;

/** Adds the alias to the person's aliases, refusing one that equals, ignoring case, any name or alias on the sheet
 *  or on another sheet of its tree (see {@link SettingStore#tree}). */
public class AddAlias {
    public void add(@Val AliasRequest request, @Val Person person, PersonStore store, SettingStore settings)
            throws DuplicateNameException {
        if (store.nameTaken(person.getSheetId(), request.alias(), null)
                || store.usedOn(settings.tree(person.getSheetId()), request.alias())) {
            throw new DuplicateNameException(request.alias());
        }
        store.addAlias(person, request.alias());
    }
}
