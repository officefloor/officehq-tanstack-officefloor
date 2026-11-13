package net.officefloor.hq.app.claim;

import jakarta.validation.constraints.Size;

/** A claim's fields as the form submits them: an amount (required), a date (blank: the sheet's entry date, else its today), a note and,
 * for a correction (a negative amount), the number of the claim it corrects. */
public record ClaimRequest(@Size(max = 30) String amount, @Size(max = 30) String date, @Size(max = 1000) String note,
        @Size(max = 120) String corrects) {
}
