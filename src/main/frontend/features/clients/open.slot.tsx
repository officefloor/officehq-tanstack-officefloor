import { Link } from '@tanstack/react-router';
import { ClientRow } from '../../slots/defs/clientRow';

// The "open this client" action on every client row — its own file (CLAUDE.md rule 3). Drilling in
// is a child route (rule 2): the link navigates to /clients/$clientId, where the client's projects
// live. Carries data-testid="client-open-<id>" (the test contract).
export const contribution = ClientRow.fill({
  order: 0,
  Component: ({ clientId }: { clientId: number }) => (
    <Link
      to="/clients/$clientId"
      params={{ clientId: String(clientId) }}
      data-testid={`client-open-${clientId}`}
    >
      Open
    </Link>
  ),
});
