# E2E tests

UI and its interaction with the backend are tested using E2E (end-to-end) cypress tests.

## Running the E2E tests

To just run it, you can execute the runE2e Gradle task. This command runs a complex task, which installs all dependencies and runs everything it needs.

```shell
./gradlew runE2e
```

To run only selected specs, pass `specs` property with a comma-separated list or glob:

```shell
./gradlew runE2e -Pspecs="**/translations/plurals.cy.ts,**/translations/singleKeyForm.cy.ts"
```

## Step-by-step run

1. Prepare the environment by the [development guide](../DEVELOPMENT.md).
2. Install dependencies:

   ```shell
   npm --prefix e2e ci
   ```

3. Run the tested environment:

   ```shell
   # Run frontend with E2E settings
   VITE_APP_API_URL=http://localhost:8201 VITE_DEV_PROXY_TARGET=http://localhost:8201 npm --prefix webapp run start -- --port 8202 --strictPort --host 127.0.0.1 --no-open
   # Run the E2E Docker services (like fake SMTP server)
   ./gradlew runDockerE2eDev
   # Run backend with e2e profile
   ./gradlew server-app:bootRun --args='--spring.profiles.active=e2e'
   # You can also do this by running the application with the E2e profile using Idea CE or Ultimate.
   # Then you will be also able to debug the backend and hotswap classes while running the tests, which can be pretty useful.
   ```

   In IntelliJ, run **Frontend E2E** alongside the backend with the `e2e` profile before running a Cypress spec.
   This frontend uses port 8202, matching Cypress's default `HOST` and the backend's default frontend URL,
   and connects to the E2E backend on port 8201. Use a Node version supported by `webapp/package.json`.
   If you choose another frontend port, also set `CYPRESS_HOST` to its URL (for example, `http://localhost:8081`)
   and `TOLGEE_E2E_FRONTEND_PORT` to its port number (`8081` in that example).

   **Frontend E2E** builds and serves the frontend rather than using Vite's development server, which can
   stall on a blank page in Cypress. Restart it after frontend changes to rebuild. The terminal equivalent is:

   ```shell
   VITE_APP_API_URL= VITE_DEV_PROXY_TARGET=http://localhost:8201 npm --prefix webapp run start:e2e
   ```

   `oauth2Consent.cy.ts` additionally needs `HOST` and `API_URL` to be the _same_ origin, because it visits the
   backend's `/oauth2/authorize` and then follows a redirect back to the SPA; Cypress treats a differing port as a
   different origin. `runE2e` points both at one port, so the spec passes there. This step-by-step flow does not,
   so run it with `CYPRESS_API_URL` set to the frontend origin and `VITE_DEV_PROXY_TARGET` pointing the vite dev
   server at the backend, or skip that spec locally and let CI cover it.

4. Run the tests:

   ```shell
   ./gradlew openE2eDev
   ```

5. Stop the environment when done:

   ```shell
   ./gradlew stopDockerE2e
   ```

## CI sharding

CI splits the specs into `E2E_TOTAL_JOBS` groups balanced by duration, using
`e2e/spec-durations.json` (seconds per spec, measured from a CI run). A spec
missing from the file counts as 30 seconds, so new specs need no entry. To see
the groups:

```shell
E2E_TOTAL_JOBS=20 ./gradlew printE2eGroups
```

`printE2eGroups` prints each group's total and the specs missing from or stale in the file.
Refresh the file when the groups drift apart: take the `Running:` timestamps
from the E2E job logs of a recent run and write the wall seconds per spec.
