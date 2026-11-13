package net.officefloor.hq.app.voidreason;

import jakarta.validation.constraints.Size;

/** A void reason as the form submits it: its text. */
public record VoidReasonRequest(@Size(max = 200) String reason) {
}
