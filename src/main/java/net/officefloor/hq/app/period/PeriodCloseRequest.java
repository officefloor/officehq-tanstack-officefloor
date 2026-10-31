package net.officefloor.hq.app.period;

/** Body of a period close or reopen: who closes or reopens it. */
public record PeriodCloseRequest(String user) {
}
