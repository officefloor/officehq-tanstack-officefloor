// How money is displayed everywhere: a dollar sign, thousands separators, and two decimal places,
// like "$1,234.50". Shared so every figure that is money reads the same, rather than each feature
// spelling out its own formatting. Composed, not branched — callers pass a number and get the
// canonical string.
export function formatMoney(amount: number): string {
  return `$${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
