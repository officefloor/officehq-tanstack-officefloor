// Shared presentational primitive: how money is rendered everywhere in the app. A monetary figure
// shows a dollar sign, thousands separators and two decimal places, like "$1,234.50" (CLAUDE.md:
// ui/ primitives are composed, never branched per feature). Every place that surfaces an amount, a
// total or an outstanding figure formats it through here, so they stay identical for free.
export function formatMoney(amount: number | string): string {
  return `$${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
