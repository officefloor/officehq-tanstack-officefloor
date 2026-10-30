import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { plansKey } from './usePlans';

// The generate button: for every plan on the sheet, in the order they were added, a DRAFT claim of the
// plan's amount for each plan date (the start, then every N days) up to the end and the sheet's today that
// has none yet, dated that day with an empty note. A date the claim form refuses is skipped and tried
// again next time. Shows how many were made; pressing again removes the old count until the new one is in.
export function GeneratePlans() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const generate = useMutation({
    mutationFn: () => postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheet)}/plans/generate`, { user }),
    onSuccess: () => {
      // Not awaited: the count shows as soon as the claims are in, never after the rows it made.
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="plan-generate"
        onClick={() =>
          once(() => {
            generate.reset();
            return generate.mutateAsync();
          })
        }
      >
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <span data-testid="plan-generated-count">{generate.data.made}</span>
        </>
      )}
      {generate.isError && (
        <span data-testid="plan-generate-error" role="alert">
          {' '}
          The plans' claims could not be generated.
        </span>
      )}
    </p>
  );
}
