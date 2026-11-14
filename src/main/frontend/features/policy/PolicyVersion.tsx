import { useSettings } from '../settings/settings';

// The sheet's current policy version: how many changes have been made to its policy settings (0: none yet).
export function PolicyVersion() {
  const settings = useSettings().data;
  return (
    <span>
      Policy version: <span data-testid="policy-version">{settings ? settings.policyVersion : ''}</span>
    </span>
  );
}
