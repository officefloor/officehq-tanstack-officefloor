import { useUser } from './useSession';

// Who is signed in, and a way to sign out.
export function UserBar() {
  const [user, setUser] = useUser();
  return (
    <div data-testid="user-bar">
      Signed in as <strong data-testid="current-user">{user}</strong>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
        Sign out
      </button>
    </div>
  );
}
