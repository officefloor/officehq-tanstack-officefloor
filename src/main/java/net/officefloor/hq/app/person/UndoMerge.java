package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Undoes the last merge into the person, by the rules of {@link PersonMerger}. */
public class UndoMerge {
    public void undo(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            PersonMerger merger) throws InvalidPersonException {
        merger.undo(sheet, user, person);
    }
}
