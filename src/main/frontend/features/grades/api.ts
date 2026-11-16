import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';
import type { Grade } from '../people/api';

/** A grade of the sheet, and whether it is retired: a retired grade cannot be chosen for a new person
 *  or in an edit (people on it keep it). */
export type SheetGrade = { grade: Grade; retired: boolean };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const gradesKey = (sheetId: string) => [...sheetKey(sheetId), 'grades'] as const;

/** The grades A, B and C in order, each with whether it is retired. */
export function listGrades(sheetId: string): Promise<SheetGrade[]> {
  return getJson<SheetGrade[]>(`${sheetBase(sheetId)}/grades`);
}

/** Retire a grade; refused (rejects) for one already retired. Resolves to the grades as they now stand. */
export function retireGrade(sheetId: string, by: string, grade: string): Promise<SheetGrade[]> {
  return postJson<SheetGrade[]>(`${sheetBase(sheetId)}/grade-retire?by=${encodeURIComponent(by)}`, { grade });
}

/** How many people a grade merge gave the other grade. */
export type GradeMerged = { merged: number };

/** Merge a grade into another: everyone on it today is given the other from the sheet's today.
 *  Refused (rejects) for the same grade twice, or for anyone but a steward. */
export function mergeGrade(sheetId: string, by: string, from: string, into: string): Promise<GradeMerged> {
  return postJson<GradeMerged>(`${sheetBase(sheetId)}/grade-merge?by=${encodeURIComponent(by)}`, { from, into });
}
