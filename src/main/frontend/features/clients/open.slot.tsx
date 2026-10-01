import { Link } from '@tanstack/react-router';
import { ClientRow } from '../../slots/defs/clientRow';

// The link that opens a client's detail page — one new file filling the clients-row action slot.
// Drilling in is a child route (routes/clients.$clientId.tsx), never a flag, so this is a plain
// navigation to that route. Its testid is client-open-<id> (the test contract; CLAUDE.md).
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
