// How money is displayed everywhere: a currency symbol, thousands separators, and two decimal
// places, like "$1,234.50" or "€1,234.50". Shared so every figure that is money reads the same,
// rather than each feature spelling out its own formatting. Composed, not branched — callers pass a
// number (and optionally the currency it is in) and get the canonical string. Each client is billed
// in their own currency, so callers showing a client's money pass that currency; the default is USD,
// the currency every figure was shown in before currencies were per-client.
const SYMBOLS: Record<string, string> = {
  USD: '$',
  EUR: '€',
  GBP: '£',
};

export function formatMoney(amount: number, currency: string = 'USD'): string {
  const symbol = SYMBOLS[currency] ?? `${currency} `;
  return `${symbol}${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
