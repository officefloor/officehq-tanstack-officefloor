package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Merges the person into another on the sheet by the rules of {@link PersonMerger}. */
public class MergePerson {
    public void merge(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val MergeRequest body,
            @Val Person person, PersonMerger merger) throws InvalidPersonException {
        merger.merge(sheet, user, person, body.into());
    }
}
