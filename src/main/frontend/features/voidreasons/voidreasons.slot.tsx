import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { voidReasonsKey, voidReasonsPath, useVoidReasons, type VoidReason } from './voidreasons';

// The sheet's void reasons, and a form that adds one. While the list is not empty, a void needs a reason chosen
// from it on the claim's row.
function VoidReasons({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: reasons = [] } = useVoidReasons(sheet);
  const [reason, setReason] = useState('');

  const add = useMutation({
    mutationFn: (body: { reason: string }) => postJson<VoidReason[]>(byUser(voidReasonsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(voidReasonsKey(sheet), updated);
      setReason('');
    },
  });
  const send = useSubmitOnce(add);

  const change = (next: string) => {
    setReason(next);
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a blank reason or one already on the list.
    send({ reason });
  };

  return (
    <section>
      <h2>Void reasons</h2>
      <form onSubmit={submit}>
        <label>
          Reason{' '}
          <input data-testid="void-reason-text" value={reason} onChange={(e) => change(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {add.isError && (
          <span data-testid="void-reason-error" role="alert">
            {' '}A void reason must not be blank nor already on the list.
          </span>
        )}
      </form>
      <ul>
        {reasons.map((r) => (
          <li key={r.reason} data-testid={`void-reason-row-${r.reason}`}>
            {r.reason}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.5, Component: VoidReasons });
