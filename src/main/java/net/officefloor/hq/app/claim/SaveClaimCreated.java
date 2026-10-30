package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SaveClaimCreated {
    public void save(@Val ValidClaim request, @Val Person person, Claims claims, Sheets sheets,
            ObjectResponse<ResponseEntity<ClaimResponse>> response) {
        String sheetId = person.getSheetId();
        Claim claim = claims.add(sheetId, person.getId(), request.amount(),
                request.date() != null ? request.date() : sheets.today(sheetId), request.note());
        response.send(new ResponseEntity<>(ClaimResponse.from(claim), HttpStatus.CREATED));
    }
}
