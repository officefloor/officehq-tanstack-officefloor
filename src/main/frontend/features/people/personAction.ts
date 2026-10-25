import { useMutationState } from '@tanstack/react-query';

/** The key every action on a person's row (a statement, a retirement) runs its mutation under, so the row shows
 *  one error at a time: the last action's. */
export const personActionKey = (sheetId: string, personId: number) => ['sheet', sheetId, 'person-action', personId] as const;

/** Whether the mutation (run under personActionKey) is the last action taken on the person's row: only that
 *  one's refusal shows, so the next action on the row removes the error of the one before. */
export function useLastPersonAction(sheetId: string, personId: number, submittedAt: number): boolean {
  const times = useMutationState({
    filters: { mutationKey: personActionKey(sheetId, personId) },
    select: (m) => m.state.submittedAt,
  });
  return submittedAt === Math.max(0, ...times);
}
