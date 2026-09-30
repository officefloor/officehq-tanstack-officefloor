import { Link } from '@tanstack/react-router';
import { ClientRowActions } from '../../slots/defs/clientRowActions';

// The link that opens a client's detail page — its own file, filling the client-row-actions slot.
// Drilling in is a child route (/clients/$clientId), reached by this Link; the list is not edited to
// know about it. Carries data-testid="client-open-<id>" (the test contract).
export const contribution = ClientRowActions.fill({
  order: 10,
  Component: ({ clientId }) => (
    <Link
      to="/clients/$clientId"
      params={{ clientId: String(clientId) }}
      data-testid={`client-open-${clientId}`}
    >
      Open
    </Link>
  ),
});
