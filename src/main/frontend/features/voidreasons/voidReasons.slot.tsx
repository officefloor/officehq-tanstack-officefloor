import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';

/** A void reason of the sheet: its text. */
export type VoidReason = { reason: string };

/** The key every feature reads the sheet's void reasons under. */
export const voidReasonsKey = (sheetId: string) => ['sheet', sheetId, 'void-reasons'] as const;

/** The sheet's void reasons, in the order added. */
export function useVoidReasons(sheetId: string) {
  return useQuery({
    queryKey: voidReasonsKey(sheetId),
    queryFn: () => getJson<VoidReason[]>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`),
  });
}

// The void reason list: a text entry adds a reason (trimmed); a blank one or one already on the list is refused. While
// the list is not empty, a claim's void needs a reason chosen from it.
function VoidReasonsPanel({ sheetId }: { sheetId: string; user: string }) {
  const reasons = useVoidReasons(sheetId);
  const [text, setText] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (value: string) => {
      if (value.trim() === '') throw new Error('Enter a void reason');
      if ((reasons.data ?? []).some((r) => r.reason === value.trim())) {
        throw new Error("That void reason is already on this sheet's list");
      }
      // `by` is the signed-in user the change is recorded against.
      return postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`,
        { reason: value.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
      setText('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <section data-testid="void-reasons-panel">
      <h2>Void reasons</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(text);
        }}
      >
        <input
          data-testid="void-reason-text"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {add.isError && (
          <span data-testid="void-reason-error" role="alert">
            {' '}
            {add.error.message}
          </span>
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

export const contribution = SheetBody.fill({ order: 8.5, Component: VoidReasonsPanel });
