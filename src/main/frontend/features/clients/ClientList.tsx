import { useClients } from './clients';

// The clients list. Reads server data straight from its query key — never copied into state, never
// handed down from a parent.
export function ClientList() {
  const { data: clients } = useClients();

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
        {clients.map((client) => (
          <tr key={client.id} data-testid={`client-row-${client.id}`}>
            <td data-testid="client-name">{client.name}</td>
            <td data-testid="client-email">{client.email}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
