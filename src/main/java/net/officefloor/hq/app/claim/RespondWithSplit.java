package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithSplit {
    public void respond(@Val SplitOff made, ObjectResponse<ResponseEntity<ClaimResponse>> response,
            ClaimRepository claims, ClaimLogRepository log, SheetRepository sheets) {
        Claim claim = made.claim();
        response.send(new ResponseEntity<>(ClaimResponse.from(claim, claims, log, sheets.today(claim.sheetId())),
                HttpStatus.CREATED));
    }
}
