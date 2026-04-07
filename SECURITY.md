# Security Policy

## Supported Versions

The `master` branch is the actively maintained version.

## Reporting a Vulnerability

If you discover a security issue, do not open a public issue with exploit details.

Please report privately by contacting the repository owner through GitHub profile contact options and include:

1. A clear description of the issue
2. Steps to reproduce
3. Potential impact
4. Suggested mitigation (if available)

## Secret Management

- Never commit secrets to source control.
- Keep local secrets in `local.properties`.
- Use GitHub repository secrets for CI/CD:
  - `API_KEY`
  - `GEMINI_API_KEY`
  - `GOOGLE_WEB_CLIENT_ID`
  - `GOOGLE_SERVICES_JSON` (base64)
  - `ANDROID_KEYSTORE_BASE64`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

## Production Hardening Recommendations

- Use strict Firestore security rules.
- Rotate keys if accidental exposure occurs.
- Keep dependencies updated and patch known CVEs.
- Run CI checks on every pull request.
