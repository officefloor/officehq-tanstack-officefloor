import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { liveKey, sheetKey } from '../sheet/api';
import { GRADES, type Grade } from '../people/api';

/** A grade of the sheet, and whether it is retired: a retired grade cannot be chosen for a new person
 *  or in an edit (people on it keep it). */
export type SheetGrade = { grade: Grade; retired: boolean };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const gradesKey = (sheetId: string) => [...sheetKey(sheetId), 'grades'] as const;

/** The grades A, B and C, then those added in the order added, each with whether it is retired. */
export function listGrades(sheetId: string, live = false): Promise<SheetGrade[]> {
  return getJson<SheetGrade[]>(`${sheetBase(sheetId)}/grades`, live);
}

/** The sheet's grade codes in order: every grade select, the summary and the rates list these. Read
 *  live: a grade, once added, is listed even while the screen shows the sheet as of an earlier date. */
export function useGradeCodes(sheetId: string): Grade[] {
  const grades = useQuery({ queryKey: liveKey(gradesKey(sheetId), true), queryFn: () => listGrades(sheetId, true) });
  return grades.data ? grades.data.map((g) => g.grade) : GRADES;
}

/** Add a grade (one to three capital letters, not yet a grade); refused (rejects) for anything else.
 *  Resolves to the grades as they now stand. */
export function addGrade(sheetId: string, by: string, grade: string): Promise<SheetGrade[]> {
  return postJson<SheetGrade[]>(`${sheetBase(sheetId)}/grade-add?by=${encodeURIComponent(by)}`, { grade });
}

/** Retire a grade; refused (rejects) for one already retired. Resolves to the grades as they now stand. */
export function retireGrade(sheetId: string, by: string, grade: string): Promise<SheetGrade[]> {
  return postJson<SheetGrade[]>(`${sheetBase(sheetId)}/grade-retire?by=${encodeURIComponent(by)}`, { grade });
}

/** How many people a grade merge gave the other grade. */
export type GradeMerged = { merged: number };

/** Merge a grade into another: everyone on it today is given the other from the sheet's today, and
 *  retroactively every grade record of theirs on it becomes the other.
 *  Refused (rejects) for the same grade twice, or for anyone but a steward. */
export function mergeGrade(sheetId: string, by: string, from: string, into: string): Promise<GradeMerged> {
  return postJson<GradeMerged>(`${sheetBase(sheetId)}/grade-merge?by=${encodeURIComponent(by)}`, { from, into });
}
