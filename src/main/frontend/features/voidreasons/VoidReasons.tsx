import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useVoidReasons, voidReasonsKey } from './voidReasons';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's void reasons and the entry a reason is added to them with; a blank reason or one already on the list is
// refused.
export function VoidReasons() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const reasons = useVoidReasons().data ?? [];
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { reason: string }) =>
      postJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setText('');
      setError('');
      queryClient.setQueryData(voidReasonsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheet) });
    },
    onError: () => setError('That void reason was refused: it is blank or already on the list.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const reason = text.trim();
    if (!reason) {
      setError('Type the void reason.');
      return;
    }
    if (reasons.includes(reason)) {
      setError(`${reason} is already a void reason.`);
      return;
    }
    once(() => add.mutateAsync({ reason }));
  };

  return (
    <section data-testid="void-reasons">
      <ul data-testid="void-reason-list">
        {reasons.map((r) => (
          <li key={r} data-testid={`void-reason-row-${r}`}>
            {r}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="void-reason-form">
        <input
          data-testid="void-reason-text"
          placeholder="Void reason"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setError('');
          }}
        />
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {error && <span data-testid="void-reason-error">{error}</span>}
      </form>
    </section>
  );
}
