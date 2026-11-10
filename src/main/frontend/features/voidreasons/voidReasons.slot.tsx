import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { useVoidReasons, voidReasonsKey } from './voidReasons';
import type { VoidReason } from './voidReasons';

// The sheet's void reasons: a text entry and an add button, each reason listed below. A blank reason,
// or one already on the list, is refused. While the list is not empty, a claim is voided only with a
// reason chosen from it.
function VoidReasons({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const reasons = useVoidReasons(sheetId);
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { reason: string }) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`, body, user),
    onSuccess: () => {
      setText('');
      return queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
    },
    onError: () => setError('That void reason could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const value = text.trim();
    if (value === '') {
      setError('A void reason is required.');
      return;
    }
    if ((reasons.data ?? []).some((r) => r.reason === value)) {
      setError(`${value} is already a void reason.`);
      return;
    }
    once((done) => add.mutate({ reason: value }, { onSettled: done }));
  };

  return (
    <section data-testid="void-reasons">
      <h2>Void reasons</h2>
      <form onSubmit={submit}>
        <label>
          Reason{' '}
          <input
            data-testid="void-reason-text"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add reason
        </button>
        {error && (
          <p role="alert" data-testid="void-reason-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="void-reason-list">
        {(reasons.data ?? []).map((r) => (
          <li key={r.reason} data-testid={`void-reason-row-${r.reason}`}>
            {r.reason}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -5.5, Component: VoidReasons });
