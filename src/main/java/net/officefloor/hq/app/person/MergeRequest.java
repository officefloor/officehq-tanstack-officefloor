package net.officefloor.hq.app.person;

/** The person another is merged into, as the row submits it (their id). */
public record MergeRequest(Long into) {
}
