package net.officefloor.hq.app.claim;

import jakarta.validation.constraints.Size;

/** A claim's fields as the form submits them: an amount (required), a date (blank: the sheet's today) and a note. */
public record ClaimRequest(@Size(max = 30) String amount, @Size(max = 30) String date, @Size(max = 1000) String note) {
}
