# Contributing to BFIT

Thank you for contributing to BFIT. This guide defines the expected workflow and quality standards.

## Development Workflow

1. Fork or branch from `master`.
2. Use focused branch names such as `feat/weight-analytics` or `fix/login-crash`.
3. Keep commits small and descriptive.
4. Open a pull request with context, screenshots (if UI changed), and testing notes.

## Commit Message Convention

Use concise prefixes when possible:

- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation
- `refactor:` internal code restructuring
- `test:` test updates
- `chore:` tooling/build updates

Example:

`feat: add 30-day trend toggle on progress chart`

## Local Validation Before PR

Run these commands locally before opening a PR:

```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

On Windows:

```powershell
./gradlew.bat lintDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
```

## UI Change Checklist

- Works on small and regular device sizes
- Works in light and dark mode
- Uses string resources (avoid hardcoded text)
- Touch targets remain accessible

## Security and Secrets

- Never commit real API keys, keystores, or credentials.
- Use `local.properties` for local keys.
- Use GitHub Actions Secrets for CI/CD and release signing.

## Pull Request Expectations

Every PR should include:

1. What changed
2. Why it changed
3. How it was tested
4. Any migration or setup impact
