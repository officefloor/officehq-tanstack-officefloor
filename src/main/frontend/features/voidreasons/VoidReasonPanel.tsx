import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useVoidReasons, voidReasonsKey } from './useVoidReasons';

// The sheet's void reasons and the form that adds one (blank, or one already on the list, is refused).
// While the list is not empty, voiding a claim needs a reason chosen from it.
export function VoidReasonPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const reasons = useVoidReasons(sheet);
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { reason: string }) =>
      postJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, { ...body, user }),
    onSuccess: (saved) => {
      setReason('');
      queryClient.setQueryData(voidReasonsKey(sheet), saved);
    },
  });
  return (
    <>
      <ul data-testid="void-reason-list">
        {reasons.map((r) => (
          <li key={r} data-testid={`void-reason-row-${r}`}>{r}</li>
        ))}
      </ul>
      <form
        data-testid="void-reason-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ reason: reason.trim() });
          });
        }}
      >
        <input
          data-testid="void-reason-text"
          value={reason}
          placeholder="Void reason"
          onChange={(e) => {
            setReason(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="void-reason-add">Add void reason</button>
        {add.isError && <span data-testid="void-reason-error" role="alert"> Could not add the void reason: it must not be blank or already on the list.</span>}
      </form>
    </>
  );
}
