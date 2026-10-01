import { asString, useSearchParam } from '../../url/useSearchParam';

// The client-name search box. It OWNS the `clientSearch` URL param — a filter outlives a click, so
// it lives in the URL, not in a parent's useState (CLAUDE.md rule 4). Anything that needs the value
// (the list) reads the same key; nothing is passed between them.
export function ClientSearch() {
  const [query, setQuery] = useSearchParam('clientSearch', asString);

  return (
    <input
      data-testid="client-search"
      type="search"
      placeholder="Search clients by name"
      value={query}
      onChange={(event) => setQuery(event.target.value)}
    />
  );
}
