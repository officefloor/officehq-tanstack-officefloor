import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { voidReasonsKey, voidReasonsQuery } from './voidReasonsQuery';
import type { VoidReason } from './voidReasonsQuery';

/** The sheet's void reasons, and the entry a reason is added through. While the list is not empty, a claim is voided
 *  only with a reason chosen from it. A blank reason, or one already on the list, is refused. */
function VoidReasons({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const reasons = useQuery(voidReasonsQuery(sheetId));
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { reason: string }) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
      setText('');
    },
    onError: (e: Error) => setError(e.message || 'The void reason could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync({ reason: text.trim() }));
  };

  return (
    <section data-testid="void-reasons">
      <h3>Void reasons</h3>
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
        <button type="submit" data-testid="void-reason-add" disabled={save.isPending}>
          Add void reason
        </button>
        {error && <p data-testid="void-reason-error">{error}</p>}
      </form>
      {reasons.isError && <p data-testid="void-reasons-load-error">Could not load the void reasons.</p>}
      <ul data-testid="void-reasons-list">
        {(reasons.data ?? []).map((r) => (
          <li key={r.reason} data-testid={`void-reason-row-${r.reason}`}>
            {r.reason}
          </li>
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.6,
  Component: ({ sheetId }) => <VoidReasons key={`void-reasons-${sheetId}`} sheetId={sheetId} />,
});
