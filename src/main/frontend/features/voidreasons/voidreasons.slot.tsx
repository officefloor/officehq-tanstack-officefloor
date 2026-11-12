import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type VoidReason, voidReasonsKey, useVoidReasons } from './voidreasons';

// The sheet's void reasons, and the entry a reason is added with. A blank reason, or one already on the
// list, is refused.
function VoidReasons() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const reasons = useVoidReasons(sheet);
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (r: string) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, { reason: r, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheet) });
      setReason('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <section data-testid="void-reasons">
      <h2>Void reasons</h2>
      <ul>
        {(reasons.data ?? []).map((r) => (
          <li key={r.reason} data-testid={`void-reason-row-${r.reason}`}>
            {r.reason}
          </li>
        ))}
      </ul>
      <form
        data-testid="void-reason-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(reason);
        }}
      >
        <input
          data-testid="void-reason-text"
          placeholder="Reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            add.reset();
          }}
        />
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {add.isError && (
          <span data-testid="void-reason-error" role="alert">
            {' '}
            {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The void reason was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47.5, Component: VoidReasons });
