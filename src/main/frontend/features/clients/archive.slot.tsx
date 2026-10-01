import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { archiveClient, clientsKey } from './api';

// Archive a client — one new file filling the per-client-row action slot. Rather than delete, the
// click tucks the client away: a useMutation (never a hand-rolled list edit in state) that, on
// success, invalidates the shared ['clients'] key so the list refetches and the archived row drops
// off the list and the search. The server flips the flag and appends the audit record, so "note it
// when I do" is handled where the side-effect lives. Its testid is client-archive-<id> (the test
// contract; CLAUDE.md).
function ArchiveClient({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => archiveClient(clientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
  });

  return (
    <button
      data-testid={`client-archive-${clientId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Archive
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 20, Component: ArchiveClient });
