import { useSearchParam, asNumber } from '../../url/useSearchParam';

// The all-invoices list is shown one page at a time. Which page you are on is state that outlives a
// click, so it lives in the URL under the `invoicePage` key (DESIGN rule 4). The pager control owns
// the key; the list reads the same key and slices to that window — the two share only the key, no
// import between them.
export const INVOICE_PAGE_SIZE = 10;

// 1-based page number, defaulting to the first page and never below it. Writing page 1 clears the key
// so the first page stays off the URL (the clean, shareable default).
export function useInvoicePage(): [number, (page: number) => void] {
  const [raw, setRaw] = useSearchParam('invoicePage', asNumber);
  const page = raw && raw >= 1 ? Math.floor(raw) : 1;
  return [page, (next) => setRaw(next <= 1 ? undefined : next)];
}
