import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useVoidReasons, voidReasonsKey } from './voidreasons';
import type { VoidReason } from './voidreasons';

function VoidReasonList({ sheetId }: { sheetId: string }) {
  const reasons = useVoidReasons(sheetId).data ?? [];
  return (
    <ul>
      {reasons.map((r) => (
        <li key={r.id} data-testid={`void-reason-row-${r.reason}`}>
          {r.reason}
        </li>
      ))}
    </ul>
  );
}

function VoidReasonForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (r: string) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`, { user, reason: r }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
      setReason('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the void reason.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (reason.trim() === '') {
      setError('Give the void reason.');
      return;
    }
    if (!once.start()) return;
    save.mutate(reason.trim());
  };

  return (
    <form data-testid="void-reason-form" onSubmit={submit}>
      <input
        data-testid="void-reason-text"
        placeholder="Void reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="void-reason-add" disabled={save.isPending}>
        Add void reason
      </button>
      {error && (
        <p data-testid="void-reason-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's void reasons and the entry a reason is added with. While the list is not empty, a claim is voided
 *  only with a reason chosen from it. */
function VoidReasons({ sheetId, user }: { sheetId: string; user: string }) {
  return (
    <section data-testid="void-reasons">
      <h2>Void reasons</h2>
      <VoidReasonForm sheetId={sheetId} user={user} />
      <VoidReasonList sheetId={sheetId} />
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.6, Component: VoidReasons });
