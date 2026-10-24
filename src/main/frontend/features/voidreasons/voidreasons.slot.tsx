import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useVoidReasons, voidReasonsPath, voidReasonsQueryKey, type VoidReason } from './voidreasons';

// The sheet's void reasons, and the entry that adds one. A blank reason, or one already on the list, is refused by
// the server and shows the error beside the entry, which goes as soon as the entry changes.
function VoidReasons() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const reasons = useVoidReasons();
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { reason: string }) => postJson<VoidReason>(voidReasonsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: voidReasonsQueryKey(sheet ?? '') });
      setReason('');
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  return (
    <section data-testid="void-reasons">
      <h2>Void reasons</h2>
      <ul>
        {(reasons.data ?? []).map((r) => (
          <li key={r.id} data-testid={`void-reason-row-${r.reason}`}>{r.reason}</li>
        ))}
      </ul>
      <form
        data-testid="void-reason-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ reason });
        }}
      >
        <input
          data-testid="void-reason-text"
          placeholder="Reason"
          value={reason}
          onChange={(e) => { setReason(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {add.isError && (
          <span data-testid="void-reason-error">A void reason must not be blank or already on the list.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.6, Component: VoidReasons });
