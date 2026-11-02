import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { isRetired, usePeople, type Grade } from '../people/people';
import { useGrades } from '../grades/grades';
import { useSheet, useSheetToday } from '../sheet/sheet';
import { useRates } from '../rates/rates';
import { useFloorCents, useSettings } from '../settings/settings';
import { personDueCents, useClaims, useLate } from '../claims/claims';
import { useCapCentsOf } from '../pools/pools';

export const sheetExistsKey = (sheet: string) => ['sheet-exists', sheet] as const;

/** Whether the sheet exists (has been loaded). Asking does not make it exist. */
export function useSheetExists(sheet: string) {
  return useQuery({
    queryKey: sheetExistsKey(sheet),
    queryFn: () => getJson<boolean>(`/api/sheets/${encodeURIComponent(sheet)}/exists`),
    enabled: sheet !== '',
  });
}

/** Per grade, the sheet's summary due-total: the due-totals of the people on it today (retired ones left out) added
 *  up, an n/a due-total adding nothing — figured as the summary figures it. `ready` once its reads have answered. */
export function useGradeDue(sheet: string): { ready: boolean; grades: Grade[]; due: Record<Grade, bigint> } {
  const gradesQ = useGrades(sheet);
  const grades = (gradesQ.data ?? []).map((g) => g.grade);
  const sheetQ = useSheet(sheet);
  const peopleQ = usePeople(sheet);
  const claimsQ = useClaims(sheet);
  const ratesQ = useRates(sheet);
  const settingsQ = useSettings(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const capOf = useCapCentsOf(sheet);
  const late = useLate(sheet);
  const people = peopleQ.data ?? [];
  const claims = claimsQ.data ?? [];
  const rates = ratesQ.data ?? [];
  const current = people.filter((p) => !isRetired(p));
  const due = Object.fromEntries(
    grades.map((g) => [
      g,
      current
        .filter((p) => p.grade === g)
        .reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, capOf(p), people) ?? 0n), 0n),
    ]),
  ) as Record<Grade, bigint>;
  const ready = [sheetQ, peopleQ, claimsQ, ratesQ, settingsQ, gradesQ].every((q) => q.isSuccess && !q.isFetching);
  return { ready, grades, due };
}
