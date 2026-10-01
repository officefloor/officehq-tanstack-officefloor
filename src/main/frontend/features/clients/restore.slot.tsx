import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { clientsKey, listClientsSorted, restoreClient, type Client } from './api';

// Bring a tucked-away client back — one new file filling the per-client-row action slot, the
// inverse of archive. It shows ONLY on archived rows (visible when "show archived" is on): it reads
// the same ['clients', 'sorted', sort] query the list reads to learn the row's archived state, so
// nothing is passed down to it. The click is a useMutation (never a hand-rolled list edit) that, on
// success, invalidates the shared ['clients'] key so the list refetches and — once "show archived"
// is off again — the client is back in the default list. The server clears the flag and appends the
// audit record. Its testid is client-restore-<id> (the test contract; CLAUDE.md).
function RestoreClient({ clientId }: { clientId: number }) {
  const [sort] = useSearchParam('clientSort', asString);
  const { data: clients } = useQuery({
    queryKey: [...clientsKey, 'sorted', sort],
    queryFn: () => listClientsSorted(sort),
  });
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => restoreClient(clientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
  });

  const client = clients?.find((c: Client) => c.id === clientId);
  if (!client?.archived) {
    return null;
  }

  return (
    <button
      data-testid={`client-restore-${clientId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Restore
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 25, Component: RestoreClient });
