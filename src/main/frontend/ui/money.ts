// How money renders across the app: a dollar sign, thousands separators, and two decimals, like
// $1,234.50. Kept in one shared place so every amount — invoice rows, the project total, the
// client statement, the dashboard outstanding total — formats identically (the test contract).
export function money(amount: number): string {
  return `$${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
