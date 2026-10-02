import { asString, useSearchParam } from '../../url/useSearchParam';

// The clients-list sort control. It OWNS the `clientSort` URL param — a sort outlives a click, so it
// lives in the URL, not a parent's useState (CLAUDE.md rule 4). The list reads the same key and
// orders itself; nothing is passed between them. Default (empty) sorts by name.
export function ClientSort() {
  const [sort, setSort] = useSearchParam('clientSort', asString);

  return (
    <label>
      Sort
      <select
        data-testid="client-sort"
        value={sort || 'name'}
        onChange={(event) => setSort(event.target.value === 'name' ? undefined : event.target.value)}
      >
        <option value="name">Name</option>
        <option value="outstanding">Amount owed</option>
      </select>
    </label>
  );
}
