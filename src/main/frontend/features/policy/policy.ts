import type { PolicyVersion } from '../settings/settings';

/**
 * The policy version in force on a date (yyyy-MM-dd): how many of the sheet's policy versions are from a date on or
 * before it; a version with no from-date counts always.
 */
export function policyVersionOn(versions: PolicyVersion[] | undefined, date: string): number {
  return (versions ?? []).filter((v) => v.from === '' || v.from <= date).length;
}
