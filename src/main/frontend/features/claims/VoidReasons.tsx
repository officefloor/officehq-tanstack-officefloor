import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { addVoidReason, listVoidReasons, voidReasonsKey } from './api';

/** The sheet's void reasons and a form to add one (trimmed; a blank or repeated one refused). While the
 *  list is not empty, voiding a claim needs a reason chosen from it. */
export function VoidReasons({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const reasons = useQuery({ queryKey: voidReasonsKey(sheetId), queryFn: () => listVoidReasons(sheetId) });
  const [text, setText] = useState('');

  const save = useMutation({
    mutationFn: (input: string) => addVoidReason(sheetId, user, input),
    onSuccess: () => {
      setText('');
      return queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Void reasons</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = text;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="void-reason-text" placeholder="Reason" value={text}
          onChange={(e) => {
            setText(e.target.value);
            save.reset();
          }} />{' '}
        <button type="submit" data-testid="void-reason-add" disabled={save.isPending}>
          Add void reason
        </button>
        {save.isError && (
          <span data-testid="void-reason-error">A void reason needs text not already on the list.</span>
        )}
      </form>
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
