import { useQuery } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientRow } from '../../slots/defs/clientRow';
import { asFlag } from '../../url/useSearchParam';
import {
  CLIENT_SEARCH_PARAM,
  CLIENT_SORT_PARAM,
  CLIENTS_SHOW_ARCHIVED_PARAM,
  archivedClientsKey,
  clientsKey,
  clientsOutstandingKey,
  fetchArchivedClients,
  fetchClients,
  fetchClientsOutstanding,
  matchesClientSearch,
} from './queries';

// The list. Queries for itself under ['clients'] — never handed its data by a parent (CLAUDE.md
// rule 5). The search box owns the filter text in the URL; the list reads the same key (rule 4) and
// filters the already-loaded rows. The sort control owns the `clientSort` key; the list reads it and
// the outstanding amounts (its own query) to order the rows. Empty state and table carry the stable
// data-testid contract.
export function ClientsTable() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });
  const { data: outstanding } = useQuery({
    queryKey: clientsOutstandingKey,
    queryFn: fetchClientsOutstanding,
  });
  const [showArchived] = useSearchParam(CLIENTS_SHOW_ARCHIVED_PARAM, asFlag);
  // The tucked-away clients — fetched only for the "show archived" view (kept off otherwise so a
  // default page never asks for them). The list shows the active clients plus these when the toggle
  // is on. Both queries live under ['clients'], so a restore that invalidates ['clients'] refreshes
  // them together (CLAUDE.md rule 5).
  const { data: archived } = useQuery({
    queryKey: archivedClientsKey,
    queryFn: fetchArchivedClients,
    enabled: showArchived,
  });
  const [query] = useSearchParam(CLIENT_SEARCH_PARAM, asString);
  const [sort] = useSearchParam(CLIENT_SORT_PARAM, asString);

  if (!clients) {
    return null;
  }

  const all = showArchived ? [...clients, ...(archived ?? [])] : clients;

  if (all.length === 0) {
    return <p data-testid="clients-empty">No clients yet.</p>;
  }

  // Owed per client, keyed by id, for the 'outstanding' order (0 when not yet loaded or unknown).
  const owedById = new Map((outstanding ?? []).map((o) => [o.clientId, Number(o.outstanding)]));
  const owed = (clientId: number) => owedById.get(clientId) ?? 0;

  const visible = all
    .filter((client) => matchesClientSearch(client, query))
    .sort((a, b) =>
      sort === 'outstanding'
        ? owed(b.id) - owed(a.id)
        : a.name.localeCompare(b.name),
    );

  return (
    <table data-testid="clients-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {visible.map((client) => (
          <tr key={client.id} data-testid={`client-row-${client.id}`}>
            <td data-testid="client-name">{client.name}</td>
            <td data-testid="client-email">{client.email}</td>
            <td>
              <ClientRow.Slot clientId={client.id} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
