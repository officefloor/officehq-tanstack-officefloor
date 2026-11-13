import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';
import { GRADES } from '../people/people';

/** A grade on the sheet and whether it is retired. A retired grade cannot be chosen for a new person or in
 *  an edit; the people already on it keep it. */
export type Grade = { grade: string; retired: boolean };

/** A grade merge done: the grade merged, the one it went into and how many people moved. */
export type GradeMerged = { from: string; into: string; merged: number };

/** Under the sheet's key, so loading the sheet again re-reads its grades. */
export const gradesKey = (sheet: string) => [...sheetKey(sheet), 'grades'] as const;

export function useGrades(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(gradesKey(sheet), asOf),
    queryFn: () => getJson<Grade[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/grades`, asOf)),
    enabled: sheet !== '',
  });
}

/** The codes of the sheet's grades, in the order the server lists them (A, B, C, then those added); A, B and C
 *  until they are read. Every grade select and per-grade figure lists these. */
export function useGradeCodes(sheet: string): string[] {
  const grades = useGrades(sheet);
  return grades.data?.map((g) => g.grade) ?? [...GRADES];
}
