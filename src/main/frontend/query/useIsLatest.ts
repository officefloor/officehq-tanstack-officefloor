import { useMutationState } from '@tanstack/react-query';
import type { MutationKey } from '@tanstack/react-query';

// Whether the mutation sent at `submittedAt` is the latest of those sharing `mutationKey`. Actions that live in
// separate components but share one error element (a row's actions) show their refusal only while it is the
// row's latest, so the next action on that row removes it.
export function useIsLatest(mutationKey: MutationKey, submittedAt: number): boolean {
  const times = useMutationState({ filters: { mutationKey }, select: (m) => m.state.submittedAt });
  return submittedAt >= Math.max(0, ...times);
}
