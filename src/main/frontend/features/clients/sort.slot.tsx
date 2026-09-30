import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { CLIENT_SORT_PARAM } from './queries';

// The "sort clients" control — its own file filling the clients.toolbar region (CLAUDE.md rule 3).
// It owns the `clientSort` URL key (rule 4): the chosen order outlives a click, so it lives in the
// URL, and the list (ClientsTable) reads the very same key to order its rows. No callback, no shared
// state — just the key. Empty key means the default, 'name'.
function ClientSort() {
  const [sort, setSort] = useSearchParam(CLIENT_SORT_PARAM, asString);
  return (
    <select
      data-testid="client-sort"
      aria-label="Sort clients"
      value={sort === 'outstanding' ? 'outstanding' : 'name'}
      onChange={(event) => setSort(event.target.value)}
    >
      <option value="name">By name</option>
      <option value="outstanding">By outstanding</option>
    </select>
  );
}

export const contribution = ClientsToolbar.fill({ order: 20, Component: ClientSort });
