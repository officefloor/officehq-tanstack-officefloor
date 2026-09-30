// Shared presentational primitive: a monetary figure with grouped thousands, like "$1,000.00"
// (CLAUDE.md: ui/ primitives are composed, never branched per feature). Complements ui/money.ts's
// plain formatMoney — use this where larger figures read better grouped (a budget, a total left).
export function formatCurrency(amount: number | string): string {
  return `$${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
