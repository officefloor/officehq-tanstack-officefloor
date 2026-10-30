import { asString, useSearchParam } from '../../url/useSearchParam';

export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

// The people list's sort: "added" (list order) when the screen is first opened; it lives in the URL
// (`sort`), so it belongs to this user's screen and loading a sheet leaves it as it is. It changes
// only the order the rows are shown in — positions, totals and actions in list order are unaffected.
export function useSortBy(): [SortKey, (key: SortKey) => void] {
  const [raw, setRaw] = useSearchParam('sort', asString);
  const key = (SORT_KEYS as readonly string[]).includes(raw) ? (raw as SortKey) : 'added';
  return [key, (value) => setRaw(value === 'added' ? undefined : value)];
}

export function SortBy() {
  const [key, setKey] = useSortBy();
  return (
    <label>
      Sort by{' '}
      <select data-testid="sort-by" value={key} onChange={(e) => setKey(e.target.value as SortKey)}>
        {SORT_KEYS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>
    </label>
  );
}
