package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Refuses an edit to a name another person on the sheet already has, or a contact another person on the sheet has.
 *  Also refuses one that would leave the person clashing: their new name, or any of their aliases, used as a name or
 *  alias on another sheet of the sheet's tree (see {@link SettingStore#tree}), and one choosing a retired grade (see
 *  {@link GradeStore#refuseChoice}). */
public class CheckNameFreeForEdit {
    public void check(@Val PersonRequest request, @Val Person person, PersonStore store, SettingStore settings,
            GradeStore grades, SheetStore sheets) throws InvalidSheetException {
        if (store.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new DuplicateNameException(request.name());
        }
        Set<String> tree = store.namesOn(settings.tree(person.getSheetId()));
        if (tree.contains(request.name().trim().toLowerCase(java.util.Locale.ROOT))) {
            throw new DuplicateNameException(request.name());
        }
        for (String alias : person.getAliases()) {
            if (tree.contains(alias.trim().toLowerCase(java.util.Locale.ROOT))) {
                throw new DuplicateNameException(alias);
            }
        }
        if (request.contact() != null && store.contactTaken(person.getSheetId(), request.contact(), person.getId())) {
            throw new DuplicateContactException(request.contact());
        }
        grades.refuseChoice(person.getSheetId(), request.grade(), person, request.gradeFrom(),
                sheets.today(person.getSheetId()));
    }
}
