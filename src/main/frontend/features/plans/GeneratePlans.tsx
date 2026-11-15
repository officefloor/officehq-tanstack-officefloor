import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { generatePlans, plansKey } from './api';

/** A button that makes the claims every plan is due (see `generatePlans`), and how many it made. The
 *  count is cleared while it runs; a refused generate makes nothing and shows an error. */
export function GeneratePlans({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const generate = useMutation({
    mutationFn: () => generatePlans(sheetId, user),
    // Not awaited: the count shows as soon as the generate is done, never after its new rows.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
  });

  return (
    <section>
      <button type="button" data-testid="plan-generate" disabled={generate.isPending}
        onClick={() => submitOnce(() => {
          generate.reset();
          return generate.mutateAsync();
        })}>
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}Made <span data-testid="plan-generated-count">{generate.data.made}</span>
        </>
      )}
      {generate.isError && (
        <span data-testid="plan-generate-error">The plans' claims could not be generated.</span>
      )}
    </section>
  );
}
