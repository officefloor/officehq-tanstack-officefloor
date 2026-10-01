import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// The clients-list sort control — one new *.slot.tsx filling the clients.toolbar region. It owns the
// `clientSort` URL key; the list reads the same key and asks the server for that order. Nothing is
// passed between them: they stay in step through the shared search param. The empty option clears the
// key, so the list falls back to its default (id) order.
function ClientSort() {
  const [sort, setSort] = useSearchParam('clientSort', asString);
  return (
    <select
      data-testid="client-sort"
      value={sort}
      onChange={(e) => setSort(e.target.value)}
    >
      <option value="">Sort…</option>
      <option value="name">By name</option>
      <option value="outstanding">By amount owed</option>
    </select>
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ClientSort });
