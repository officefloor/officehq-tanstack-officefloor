// How money is shown across the app: a currency symbol and two decimal places, e.g. $100.00 for USD
// or €100.00 for EUR. One shared formatter so every amount — an invoice, a project total, the
// dashboard outstanding — reads the same way. Each client has their own currency (Flyway V31), so the
// currency is a parameter; it defaults to USD, which is how every amount read before clients could be
// billed in anything else. Amounts arrive from the server as numbers (or numeric strings); coerce first.
const formatters = new Map<string, Intl.NumberFormat>();

function formatterFor(currency: string): Intl.NumberFormat {
  let formatter = formatters.get(currency);
  if (!formatter) {
    formatter = new Intl.NumberFormat('en-US', { style: 'currency', currency });
    formatters.set(currency, formatter);
  }
  return formatter;
}

export function formatMoney(amount: number | string, currency: string = 'USD'): string {
  return formatterFor(currency).format(Number(amount));
}
