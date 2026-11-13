package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person the undo restored, as they stand now. */
public class RespondWithMergeUndone {
    public void respond(@Val MergeUndone undone, StatementStore statements, PatronStore patrons, SheetStore sheets,
            SettingStore settings, PersonStore store, ObjectResponse<PersonResponse> response) {
        Person person = undone.person();
        response.send(PersonResponse.from(person, sheets.today(person.getSheetId()),
                statements.summary(person.getSheetId(), person.getId()),
                patrons.of(person.getSheetId(), person.getId()),
                PersonStore.clashes(person, store.namesOn(settings.tree(person.getSheetId())))));
    }
}
