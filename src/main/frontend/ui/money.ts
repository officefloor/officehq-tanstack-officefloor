// How money renders across the app: a currency symbol, thousands separators, and two decimals, like
// $1,234.50 or €1,234.50. Kept in one shared place so every amount — invoice rows, the project
// total, the client statement, the dashboard totals, the top-clients list — formats identically (the
// test contract). Each client is billed in their own currency, so callers pass the currency to show;
// it defaults to USD so amounts with no client attached still render the way they always did.
const SYMBOLS: Record<string, string> = { USD: '$', EUR: '€' };

export function money(amount: number, currency: string = 'USD'): string {
  const symbol = SYMBOLS[currency] ?? SYMBOLS.USD;
  return `${symbol}${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
