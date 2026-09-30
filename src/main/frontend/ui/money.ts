// Shared presentational primitive: how money is rendered everywhere in the app. A monetary figure
// shows its currency's symbol, thousands separators and two decimal places, like "$1,234.50" or
// "€1,234.50" (CLAUDE.md: ui/ primitives are composed, never branched per feature). Every place that
// surfaces an amount, a total or an outstanding figure formats it through here, so they stay
// identical for free. The currency is optional and defaults to USD, so existing callers are
// unchanged; a caller that knows a client's currency passes it and gets that currency's symbol.
const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$',
  EUR: '€',
};

export function currencySymbol(currency: string | undefined): string {
  return (currency && CURRENCY_SYMBOLS[currency]) ?? '$';
}

export function formatMoney(amount: number | string, currency?: string): string {
  return `${currencySymbol(currency)}${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
