// How money renders across the app: a dollar sign and two decimals, like $100.00. Kept in one
// shared place so every amount — invoice rows, the project total, the dashboard outstanding total —
// formats identically (the test contract).
export function money(amount: number): string {
  return `$${amount.toFixed(2)}`;
}
