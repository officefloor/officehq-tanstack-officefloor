package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildPerson {
    public void build(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            People people, Grades grades, Out<Person> person) throws InvalidSheetRequestException {
        grades.refuseRetired(sheetId, request.grade());
        person.set(built(sheetId, request, people));
    }

    /** The new person the checked request makes, or refused when its name or contact is taken. */
    static Person built(String sheetId, PersonRequest request, People people) throws InvalidSheetRequestException {
        if (people.nameUsedInTree(sheetId, request.name(), null)) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet");
        }
        if (request.contactKey() != null && people.contactTaken(sheetId, request.contactKey(), null)) {
            throw new InvalidSheetRequestException("The contact is already in use on this sheet");
        }
        Person built = new Person(null, sheetId, request.name(), request.grade());
        if (request.termsFromDate() != null) built.getDatedTerms().put(request.termsFromDate(), request.termsDays());
        else built.setTerms(request.termsDays());
        built.setContact(request.contactKey());
        built.setTags(request.tagsKey());
        return built;
    }
}
