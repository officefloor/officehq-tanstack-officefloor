package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The row's grade as the person form's edit of the loaded person: its name kept, the grade from the sheet's today,
 *  terms, contact and tags left as they are. */
public class BuildInlineGradeEdit {
    public void build(@Val InlineGradeRequest request, @Val Person loaded, Out<PersonRequest> edit) {
        edit.set(new PersonRequest(request.user(), loaded.name(), request.grade(), request.version(), null, null, null,
                null));
    }
}
