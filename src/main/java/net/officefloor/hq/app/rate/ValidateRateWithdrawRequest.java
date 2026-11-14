package net.officefloor.hq.app.rate;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRateWithdrawRequest {
    public void validate(@RequestBody @Valid RateWithdrawRequest request, Out<RateWithdrawRequest> body)
            throws InvalidRateException {
        String grade = request.grade() == null ? "" : request.grade().trim().toUpperCase();
        if (!GradeRepository.CODE.matcher(grade).matches()) {
            throw new InvalidRateException("The grade must be one to three letters.");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be yyyy-mm-dd.");
            }
        }
        body.set(new RateWithdrawRequest(grade, from.isEmpty() ? null : from));
    }
}
