import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientSummaryKey, getClientSummary } from './api';

// At-a-glance counts for one client — a small badge panel filling the top of the client.detail
// region (order 10, above the projects and contacts tables). Reads server data under
// ['clients', clientId, 'summary'] (never copied into state); the server does the counting, so this
// just renders the two figures the client-projects-count / client-contacts-count anchors expose.
function ClientCounts({ clientId }: { clientId: number }) {
  const { data } = useQuery({
    queryKey: clientSummaryKey(clientId),
    queryFn: () => getClientSummary(clientId),
  });

  if (!data) {
    return null;
  }

  return (
    <dl data-testid="client-counts">
      <dt>Projects</dt>
      <dd data-testid="client-projects-count">{data.projects}</dd>
      <dt>Contacts</dt>
      <dd data-testid="client-contacts-count">{data.contacts}</dd>
    </dl>
  );
}

export const contribution = ClientDetail.fill({ order: 10, Component: ClientCounts });
