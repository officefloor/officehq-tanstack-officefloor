import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { peopleKey, type Person } from './usePeople';

// The person's approver (a user name; nothing for none) and the form that sets it. The approver may approve or reject
// the person's PENDING claims as a steward may. A viewer, or a name that is not a single word, is refused with the
// person's error.
export function ApproverControl({ personId, approver }: { personId: string; approver: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [name, setName] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (value: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/approver`, {
        approver: value,
        user,
      }),
    onSuccess: () => {
      setName('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const taken = usePersonAction(personId, () => save.reset());

  return (
    <>
      <span data-testid="person-approver">{approver}</span>{' '}
      <input
        data-testid="person-approver-input"
        value={name}
        placeholder="Approver"
        onChange={(e) => {
          setName(e.target.value);
          save.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-approver-save"
        onClick={() => {
          taken();
          once(() => save.mutateAsync(name.trim()));
        }}
      >
        Set approver
      </button>
      {save.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The approver could not be set.
        </span>
      )}
    </>
  );
}
