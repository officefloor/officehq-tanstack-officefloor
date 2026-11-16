package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person form's edit the row's grade save makes: from the person's current version, their name, terms, contact and
 *  tags kept, the grade chosen, its from-date blank (the sheet's today). */
public class BuildInlineGrade {
    public void build(@Val Person person, @Val InlineGradeValues request, Out<PersonValues> values) {
        values.set(new PersonValues(person.name(), request.grade(), request.by(), person.version(), null, person.terms(),
                person.contact(), person.tags()));
    }
}
