import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The clients name search box — its own file, filling the clients toolbar slot. It OWNS the
// `clientSearch` URL key (rule 4: a filter outlives a click, so it lives in the URL, not useState).
// The clients list reads the same key to fetch a narrowed list; nothing is passed between them.
// Carries data-testid="client-search" (the test contract).
function ClientSearch() {
  const [q, setQ] = useSearchParam('clientSearch', asString);
  return (
    <input
      data-testid="client-search"
      type="search"
      placeholder="Search clients by name"
      value={q}
      onChange={(e) => setQ(e.target.value)}
    />
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ClientSearch });
