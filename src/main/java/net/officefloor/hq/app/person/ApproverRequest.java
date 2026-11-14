package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/** An approver as the row submits it: a user name (blank: none). */
public record ApproverRequest(@Size(max = 100) String user) {
}
