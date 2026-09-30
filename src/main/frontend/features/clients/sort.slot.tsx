import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The clients sort control — its own file, filling the clients toolbar slot. It OWNS the
// `clientSort` URL key (rule 4: a sort outlives a click, so it lives in the URL, not useState). The
// clients list reads the same key and asks the server for the ordered list; nothing is passed
// between them. Carries data-testid="client-sort" (the test contract). The empty option clears the
// key, restoring the default added-order list.
function ClientSort() {
  const [sort, setSort] = useSearchParam('clientSort', asString);
  return (
    <select
      data-testid="client-sort"
      value={sort}
      onChange={(e) => setSort(e.target.value || undefined)}
    >
      <option value="">Sort: added order</option>
      <option value="name">By name</option>
      <option value="outstanding">By amount owed</option>
    </select>
  );
}

export const contribution = ClientsToolbar.fill({ order: 20, Component: ClientSort });
