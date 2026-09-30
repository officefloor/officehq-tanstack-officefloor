import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { ClientDetail } from '../../slots/defs/clientDetail';
import type { Client } from './ClientsPage';

// Setting a client's billing currency — its own file, filling the client-detail slot. The detail
// route was written once and is not touched to add this; the panel queries the client for itself
// (rule 5, the shared ['clients'] key) and writes with a useMutation that POSTs to
// /api/clients/currency, then invalidates the keys everything that shows the client's money reads —
// ['clients'] (this panel), ['invoices'] (rows, statement, project total), ['dashboard'] (home
// totals and top clients) — so each refetches in the new currency without any import between them.
// The currencies offered match the set the server supports (ClientsSetCurrency). Carries the
// data-testid anchors the test drives: client-currency (the saved value), client-currency-select and
// client-currency-save.
const CURRENCIES = ['USD', 'EUR'];

function ClientCurrencyPanel({ clientId }: { clientId: number }) {
  const clients = useQuery({
    queryKey: ['clients'],
    queryFn: () => getJson<Client[]>('/api/clients'),
  });
  const client = clients.data?.find((c) => c.id === clientId);
  if (!client) {
    return null;
  }
  return <CurrencyForm key={client.id} client={client} />;
}

function CurrencyForm({ client }: { client: Client }) {
  const queryClient = useQueryClient();
  // useState only for the choice the user is currently making in the select (rule 4), seeded once at
  // mount from the client's saved currency. The displayed value below reads the saved client, not
  // this — so it only changes once the save lands and the list refetches.
  const [choice, setChoice] = useState(client.currency);

  const save = useMutation({
    mutationFn: () =>
      postJson<Client>('/api/clients/currency', { id: client.id, currency: choice }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  return (
    <section data-testid="client-currency-panel">
      <p>
        <span>Currency</span>
        <span data-testid="client-currency">{client.currency}</span>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate();
        }}
      >
        <select
          data-testid="client-currency-select"
          value={choice}
          onChange={(e) => setChoice(e.target.value)}
        >
          {CURRENCIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <button data-testid="client-currency-save" type="submit" disabled={save.isPending}>
          Save
        </button>
      </form>
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 5, Component: ClientCurrencyPanel });
