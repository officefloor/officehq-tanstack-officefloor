package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, StatementStore statements, PatronStore patrons, SheetStore sheets,
            SettingStore settings, PersonStore store,
            ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, sheets.today(person.getSheetId()),
                statements.summary(person.getSheetId(), person.getId()),
                patrons.of(person.getSheetId(), person.getId()),
                PersonStore.clashes(person, store.namesOn(settings.tree(person.getSheetId())))));
    }
}
