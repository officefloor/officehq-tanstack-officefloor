import { useQuery } from '@tanstack/react-query';
import { clientsKey, listClients, type Client } from './api';

// The clients list. Reads server data under ['clients'] (never copied into state); the create form
// shares the key, so a successful create refreshes this list with no import between them.
export function ClientsList() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClients });

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return <p data-testid="clients-empty">No clients yet.</p>;
  }

  return (
    <table data-testid="clients-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
        </tr>
      </thead>
      <tbody>
        {clients.map((client: Client) => (
          <tr key={client.id} data-testid={`client-row-${client.id}`}>
            <td data-testid="client-name">{client.name}</td>
            <td data-testid="client-email">{client.email}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
