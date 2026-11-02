package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Clones the sheet to another id by the rules of {@link SheetCloner}. */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val CloneRequest body, SheetCloner cloner) throws InvalidCloneException {
        cloner.cloneTo(sheet, user, body.to());
    }
}
