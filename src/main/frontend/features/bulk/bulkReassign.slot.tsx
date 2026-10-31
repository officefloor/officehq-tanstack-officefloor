import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople } from '../people/usePeople';
import { useSelectedClaims } from './useSelectedClaims';

/**
 * Move the ticked claims to another person together. Each is moved in list order (people in list
 * order, each person's newest claim first) by the rules of moving one claim; each that cannot is
 * left as it is. Shows how many were moved and how many refused; the counts go while the next
 * press runs.
 */
function BulkReassign({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  const [selected, setSelected] = useSelectedClaims();
  const [to, setTo] = useState('');
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ reassigned: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-reassign`, {
        claimIds,
        toPersonId: to === '' ? null : Number(to),
        user,
      }),
    onSuccess: () => setSelected([]),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  return (
    <section data-testid="bulk-reassign-panel">
      <select
        data-testid="bulk-reassign-to"
        aria-label="Reassign selected to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          bulk.reset();
        }}
      >
        <option value="">reassign to…</option>
        {everyone.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Reassign selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Reassigned <strong data-testid="bulk-reassigned">{bulk.data.reassigned}</strong>, refused{' '}
          <strong data-testid="bulk-reassign-refused">{bulk.data.refused}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="bulk-reassign-error" role="alert">
          {' '}
          {selected.length === 0
            ? 'Select the claims to reassign.'
            : to === ''
              ? 'Choose the person to reassign the selected claims to.'
              : 'Could not reassign the selected claims.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 16.5, Component: BulkReassign });
