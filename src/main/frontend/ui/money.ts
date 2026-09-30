// Shared presentational primitive: how money is rendered everywhere in the app. A monetary figure
// shows a dollar sign and two decimal places, like "$100.00" (CLAUDE.md: ui/ primitives are
// composed, never branched per feature). Every place that surfaces an amount, a total or an
// outstanding figure formats it through here, so they stay identical for free.
export function formatMoney(amount: number | string): string {
  return `$${Number(amount).toFixed(2)}`;
}
