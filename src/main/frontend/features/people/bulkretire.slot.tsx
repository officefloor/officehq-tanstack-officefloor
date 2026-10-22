import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { useSheetToday } from '../sheet/sheet';
import { peopleKey, usePeople, useShowRetired } from './people';
import { clearSelectedPeople, useSelectedPeople } from './selection';

type BulkRetireResult = { retired: number };

/** The bar that retires every selected person together from a date (blank: the sheet's today). The server retires
 *  each, in list order, as the row's retire would; the bar then shows how many were retired. */
function BulkRetire({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [showRetired] = useShowRetired();
  const chosen = useSelectedPeople(sheetId);
  const ids = people.filter((p) => (showRetired || p.retiredFrom === null) && chosen.has(p.id)).map((p) => p.id);
  const today = useSheetToday(sheetId);
  const [from, setFrom] = useState('');
  const [result, setResult] = useState<BulkRetireResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (body: { user: string; personIds: number[]; from: string }) =>
      postJson<BulkRetireResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-retire`, body),
    onSuccess: async (r) => {
      clearSelectedPeople(sheetId);
      setFrom('');
      setResult(r);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not retire the selected people.'),
    onSettled: once.done,
  });

  return (
    <p>
      <label>
        Retire selected from{' '}
        <input
          data-testid="bulk-retire-date"
          placeholder={today || 'yyyy-mm-dd'}
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
            setResult(null);
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={retire.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the people to retire.');
            return;
          }
          if (!once.start()) return;
          retire.mutate({ user, personIds: ids, from: from.trim() });
        }}
      >
        Retire
      </button>
      {result && (
        <>
          {' '}Retired <span data-testid="bulk-retired">{result.retired}</span>
        </>
      )}
      {error && (
        <span data-testid="person-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11.6, Component: BulkRetire });
