package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheetId}/plans/{planId}/pause and /resume: who pauses or resumes. */
public record PauseRequest(String user) {
}
