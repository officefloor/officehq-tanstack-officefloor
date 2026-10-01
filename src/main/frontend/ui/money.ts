// How money is shown across the app: a currency symbol and two decimal places, e.g. $100.00.
// One shared formatter so every amount — an invoice, a project total, the dashboard outstanding —
// reads the same way. Amounts arrive from the server as numbers (or numeric strings); coerce first.
const moneyFormat = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function formatMoney(amount: number | string): string {
  return moneyFormat.format(Number(amount));
}
