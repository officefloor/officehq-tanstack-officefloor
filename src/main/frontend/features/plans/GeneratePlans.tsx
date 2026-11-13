import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { plansKey } from './plans';
import type { Plan } from './plans';

type GenerateResult = { made: number };

// The generate button: for every plan on the sheet, a DRAFT claim for each plan date up to its end and the sheet's
// today that it has made none for yet (a date the claim form refuses is tried again next time). Shows how many were made.
export function GeneratePlans() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [error, setError] = useState('');
  const [made, setMade] = useState<number | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const generate = useMutation({
    mutationFn: () =>
      postJson<GenerateResult>(
        `/api/sheets/${encodeURIComponent(sheet)}/generate-plans?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async (result) => {
      // Re-read people, claims and plans first, then show them with the count in one render.
      const base = `/api/sheets/${encodeURIComponent(sheet)}`;
      const [people, claims, plans] = await Promise.all([
        getJson<Person[]>(`${base}/people`),
        getJson<Claim[]>(`${base}/claims`),
        getJson<Plan[]>(`${base}/plans`),
      ]);
      queryClient.setQueryData(peopleKey(sheet), people);
      queryClient.setQueryData(claimsKey(sheet), claims);
      queryClient.setQueryData(plansKey(sheet), plans);
      setMade(result.made);
    },
    onError: () => setError('The plans could not be generated.'),
  });

  if (!user || !sheet) return null;

  const submit = () => {
    setError('');
    setMade(undefined);
    once(() => generate.mutateAsync());
  };

  return (
    <div>
      <button type="button" data-testid="plan-generate" disabled={generate.isPending} onClick={submit}>
        Generate plan claims
      </button>
      {made !== undefined && (
        <>
          {' '}
          Made <span data-testid="plan-generated-count">{made}</span>
        </>
      )}
      {error && <span data-testid="plan-generate-error">{error}</span>}
    </div>
  );
}
