import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { type Person } from './usePeople';

// Transfers the person to another sheet by id (a steward only): they arrive there with their grades,
// names, contact and claims, and leave this sheet. A retired person, this sheet's own id, or a name,
// alias or contact already on the other sheet is refused. A refusal shows the person's error.
export function TransferControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [toSheet, setToSheet] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const transfer = useMutation({
    mutationFn: (to: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/transfer`, {
        toSheet: to,
        user,
      }),
    onSuccess: () => {
      setToSheet('');
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheet] });
    },
  });
  const taken = usePersonAction(personId, () => transfer.reset());

  return (
    <>
      <input
        type="text"
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={toSheet}
        onChange={(e) => {
          setToSheet(e.target.value);
          transfer.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        onClick={() => {
          taken();
          once(() => transfer.mutateAsync(toSheet.trim()));
        }}
      >
        Transfer
      </button>
      {transfer.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be transferred.
        </span>
      )}
    </>
  );
}
