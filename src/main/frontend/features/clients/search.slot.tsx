import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { CLIENT_SEARCH_PARAM } from './queries';

// The search box — its own file filling the clients.toolbar region (CLAUDE.md rule 3). It owns the
// `clientSearch` URL key (rule 4): the filter outlives a click, so it lives in the URL, and the list
// (ClientsTable) reads the very same key. No callback, no shared state — just the key.
function ClientSearch() {
  const [query, setQuery] = useSearchParam(CLIENT_SEARCH_PARAM, asString);
  return (
    <input
      data-testid="client-search"
      type="search"
      aria-label="Search clients by name"
      placeholder="Search clients by name"
      value={query}
      onChange={(event) => setQuery(event.target.value)}
    />
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ClientSearch });
