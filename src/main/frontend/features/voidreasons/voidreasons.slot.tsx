import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useVoidReasons, voidReasonsKey, type VoidReason } from './voidreasons';

/** The sheet's void reasons: the text entry and button that add one, and each reason on the list. A blank reason or
 *  one already on the list is refused. While the list is not empty, voiding a claim needs a reason chosen from it. */
function VoidReasons({ sheetId, user }: { sheetId: string; user: string }) {
  const reasons = useVoidReasons(sheetId);
  const queryClient = useQueryClient();
  const [text, setText] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (r: string) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`, { reason: r, by: user }),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="void-reasons">
      <h3>Void reasons</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(text);
        }}
      >
        <input
          data-testid="void-reason-text"
          placeholder="Reason"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {add.isError && <span data-testid="void-reason-error"> A void reason must not be blank nor already on the list.</span>}
      </form>
      {reasons.isError && <p data-testid="void-reasons-load-error">Could not load void reasons.</p>}
      <ul>
        {(reasons.data ?? []).map((r) => (
          <li key={r.reason} data-testid={`void-reason-row-${r.reason}`}>
            {r.reason}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 9.5, Component: VoidReasons });
