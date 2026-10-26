import { useUser } from './useUser';

export function CurrentUser() {
  const [user, setUser] = useUser();
  return (
    <p>
      Signed in as <strong data-testid="current-user">{user}</strong>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
        Sign out
      </button>
    </p>
  );
}
