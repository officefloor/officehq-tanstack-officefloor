package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePersonCreated {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, People people, Audit audit, @Val RollupFigures before, Rollup rollup,
            ObjectResponse<ResponseEntity<PersonResponse>> response) throws InvalidPersonException {
        if (people.nameTaken(sheetId, request.name(), null)) {
            throw new InvalidPersonException("The name is already in use");
        }
        if (!request.contact().isEmpty() && people.contactTaken(sheetId, request.contact(), null)) {
            throw new InvalidPersonException("The contact is already in use");
        }
        Person person = people.add(sheetId, request.name(), request.grade(),
                request.terms().isEmpty() ? null : Integer.valueOf(request.terms()),
                request.contact().isEmpty() ? null : request.contact());
        audit.recordOnCommit("PERSON_ADDED sheet=" + sheetId + " by=" + request.user()
                + " name=" + person.getName() + " grade=" + person.getGrade()
                + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                + (person.getContact() == null ? "" : " contact=" + person.getContact()));
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(new ResponseEntity<>(PersonResponse.from(person), HttpStatus.CREATED));
    }
}
