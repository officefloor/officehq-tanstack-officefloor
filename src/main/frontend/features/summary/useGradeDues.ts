import { useSheetTotals } from '../claims/useSheetTotals';
import { useGradeCodes } from '../grades/useGrades';
import { gradeOn } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';

/**
 * Per grade of the sheet, the people not retired on it on the sheet's today, each with their due-total in cents
 * (null when n/a), worked out as the list works it out (their parts of the claims shared with them
 * included, see useSheetTotals). One place, so the summary and the consolidated figures agree.
 */
export function useGradeDues(sheet: string) {
  const { perPerson } = useSheetTotals(sheet);
  const today = useSheetToday(sheet);
  const grades = useGradeCodes(sheet);
  const people = perPerson.filter((d) => !d.person.retired);
  return Object.fromEntries(
    grades.map((g) => [g, people.filter((d) => gradeOn(d.person, today) === g).map(({ person, due }) => ({ person, due }))]),
  ) as Record<string, Array<{ person: (typeof people)[number]['person']; due: bigint | null }>>;
}
