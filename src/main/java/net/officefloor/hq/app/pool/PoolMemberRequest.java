package net.officefloor.hq.app.pool;

/**
 * The pool to add to, the member joining it — the id of a person, or else the name of another pool of the sheet
 * ({@code memberPool}) — their share percent as typed, the membership's from-date and to-date as typed (each optional,
 * YYYY-MM-DD), and the signed-in user (`by`).
 */
public record PoolMemberRequest(String pool, Long person, String memberPool, String share, String from, String to, String by) {
}
