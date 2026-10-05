# AGENTS.md

This file provides Tolgee-specific guidance for AI coding agents working on the Tolgee localization platform.

## Repository Structure

This project uses a multi-repository setup managed by a wrapper repository:

```
tolgee-platform/                    # platform-dev-start (wrapper repo)
├── .git/                           # git@github.com:tolgee/platform-dev-start.git
├── start.sh                        # Development startup script
├── public/                         # tolgee-platform (main repo)
│   ├── .git/                       # git@github.com:tolgee/tolgee-platform.git
│   ├── backend/                    # Kotlin/Spring Boot backend
│   ├── webapp/                     # React frontend
│   └── e2e/                        # Cypress E2E tests
└── billing/                        # billing (private repo)
    └── .git/                       # git@github.com:tolgee/billing.git
```

**Important**: When working with git commands, ensure you're in the correct repository:
- For platform code changes: `cd public/` then run git commands
- For billing changes: `cd billing/` then run git commands
- The wrapper repo (`platform-dev-start`) ignores `public/` and `billing/` in its `.gitignore`

## Backend Development

### Database Migrations
After modifying JPA entities, always run:
```bash
./gradlew diffChangeLog
```
This generates Liquibase changelog entries. If you get "docker command not found", add `--no-daemon` flag.

### Backend Testing
Tests are split into multiple categories that run in parallel in CI:
```bash
./gradlew server-app:runContextRecreatingTests && \
./gradlew server-app:runStandardTests && \
./gradlew server-app:runWebsocketTests && \
./gradlew server-app:runWithoutEeTests && \
./gradlew ee-test:test && \
./gradlew data:test && \
./gradlew security:test
```

Don't use the bare test task (it doesn't work) – always run a specific test suite even when running a single test, e.g:
```bash
# Don't do this
./gradlew test --tests "io.tolgee.unit.formats.android.out.AndroidSdkFileExporterTest"

# Do this
./gradlew :data:test --tests "io.tolgee.unit.formats.android.out.AndroidSdkFileExporterTest"
```

### Running Tests with Visible Output
To see test output in real-time (like in an IDE), use `--console=plain` with grep to filter relevant logs:
```bash
# Run a specific test with visible INFO logs
./gradlew :server-app:test --tests "io.tolgee.batch.SomeTest" --console=plain --info 2>&1 | grep -E "(INFO.*SomeTest|ERROR|WARN)" | head -100

# See all test output without filtering
./gradlew :server-app:test --tests "io.tolgee.batch.SomeTest" --console=plain --info 2>&1 | tail -200
```

Use `logger.info()` in tests for diagnostic output that will be visible with these commands.

**TestData Pattern**: Use TestData classes for test setup:
```kotlin
class YourControllerTest {
  @Autowired
  lateinit var testDataService: TestDataService

  lateinit var testData: YourTestData

  @BeforeEach
  fun setup() {
    testData = YourTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }
}
```

**JSON Response Testing**: Use `.andAssertThatJson` for API responses:
```kotlin
performProjectAuthGet("items").andAssertThatJson {
  node("_embedded.items") {
    node("[0].id").isEqualTo(1)
    node("[0].name").isEqualTo("Item name")
  }
  node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
}
```

### Code Formatting
Always run before commits:
```bash
./gradlew ktlintFormat
```

## Frontend Development

### Path Aliases
Tolgee uses custom TypeScript path aliases instead of relative imports:
- `tg.component/*` → `component/*`
- `tg.service/*` → `service/*`
- `tg.hooks/*` → `hooks/*`
- `tg.views/*` → `views/*`
- `tg.globalContext/*` → `globalContext/*`

Example: `import { useUser } from 'tg.hooks/useUser'`

### API Schema Regeneration
After backend API changes, regenerate TypeScript types. **Backend must be running first**:
```bash
# 1. Start backend (in separate terminal)
./gradlew server-app:bootRun --args='--spring.profiles.active=dev'

# 2. Regenerate schemas
cd webapp
npm run schema        # For main API
npm run billing-schema # For billing API (if applicable)
```

### API Communication
Use typed React Query hooks from `useQueryApi.ts` (not raw React Query):
```typescript
// Query example
const { data, isLoading } = useApiQuery({
  url: '/v2/projects/{projectId}/languages',
  method: 'get',
  path: { projectId: project.id },
});

// Mutation example
const mutation = useApiMutation({
  url: '/v2/projects/{projectId}/languages',
  method: 'post',
  invalidatePrefix: '/v2/projects',
});

const handleSubmit = (data) => {
  mutation.mutate({
    path: { projectId: project.id },
    content: data,
  });
};
```

### Business Event Tracking
Use Tolgee-specific hooks for analytics:
```typescript
import { useReportEvent } from 'tg.hooks/useReportEvent';

const reportEvent = useReportEvent();
reportEvent('event_name', { key: 'value' });

// For component mount events:
import { useReportOnce } from 'tg.hooks/useReportEvent';
useReportOnce('page_viewed', { pageName: 'settings' });
```

## Testing

### Running Cypress Specs from IntelliJ

When running a Cypress spec directly from the IntelliJ gutter or a Cypress run configuration (for example, `e2e/cypress/e2e/llmProviders/llmProviders.cy.ts`), start the tested environment first. The current Cypress run configurations have no before-launch tasks to start the servers automatically.

1. Start the backend with the `e2e` Spring profile on port **8201** and wait for startup to finish:
   ```bash
   ./gradlew server-app:bootRun --args='--spring.profiles.active=e2e'
   ```
   Alternatively, use an IntelliJ backend configuration with the `e2e` profile. Start supporting services such as fake SMTP when needed with `./gradlew runDockerE2eDev`.
2. Run IntelliJ's **Frontend E2E** configuration (`.run/Frontend E2E.run.xml`) and wait for its build and preview server to start on port **8202**.
3. Run the Cypress spec or individual test from IntelliJ.

**Frontend E2E** runs `npm run start:e2e` from `webapp`, which builds the frontend and serves it using `vite preview --port 8202 --strictPort --host 127.0.0.1`. It uses an empty `VITE_APP_API_URL` and `VITE_DEV_PROXY_TARGET=http://localhost:8201` to proxy API requests to the E2E backend.

The defaults in `e2e/cypress/common/constants.ts` are `HOST=http://localhost:8202` for the browser UI and `API_URL=http://localhost:8201` for test setup and API calls. Keep the servers running across test runs; restart **Frontend E2E** after frontend code changes to rebuild the served files.

Keep the environment pairs aligned:

- Normal development: **Frontend** on port **3000** and **Application dev profile** backend on port **8080**.
- Direct Cypress testing: **Frontend E2E** on port **8202** and backend with the **e2e** profile on port **8201**.

A backend running only on port 8201 does not satisfy the normal frontend's proxy target on port 8080; this can produce HTTP 500 proxy responses and the UI's generic `Loadable error`.

**Alternative:** IntelliJ's **E2E** Gradle configuration runs `runE2e`, which starts the Docker test environment automatically and directs Cypress to that environment. This workflow does not require manually starting **Frontend E2E**. See `e2e/README.md` for details.

### E2E Test Data Setup
Creating E2E test data requires **3 components**:

1. **TestData Class** (`backend/data/src/main/kotlin/io/tolgee/development/testDataBuilder/data/YourFeatureTestData.kt`):
```kotlin
class YourTestData : BaseTestData() {
  val specificEntity: Entity

  init {
    root.apply {
      specificEntity = addEntity {
        name = "Test Entity"
      }.self
    }
  }
}
```

2. **E2E Data Controller** (`backend/development/src/main/kotlin/io/tolgee/controllers/internal/e2eData/YourFeatureE2eDataController.kt`):
```kotlin
@RestController
@RequestMapping("/api/internal/e2e-data/your-feature")
class YourFeatureE2eDataController : AbstractE2eDataController() {
  // Implement data generation endpoints
}
```

3. **Frontend Test Data Object** (`e2e/cypress/common/apiCalls/testData/testData.ts`):
```typescript
export const yourFeatureTestData = generateTestDataObject('your-feature');
```

Usage in tests:
```typescript
beforeEach(() => {
  yourFeatureTestData.clean();
  yourFeatureTestData.generateStandard().then((r) => {
    const testData = r.body;
    // Use testData in your tests
  });
});
```

**Note**: Use `generateStandard()`, not `generate()` (outdated pattern).

### data-cy Attributes (CRITICAL)
**STRICTLY ENFORCED**: Always use `data-cy` attributes for selectors, never text content.

- All data-cy values are typed in `e2e/cypress/support/dataCyType.d.ts` (auto-generated, don't modify)
- Use typed helpers: `gcy('...')` or `cy.gcy('...')`
- Add data-cy to all components accessed from tests
- Make data-cy attributes specific and descriptive

Example:
```tsx
// Component
<Alert severity="error" data-cy="signup-error-seats-spending-limit">
  <T keyName="spending_limit_dialog_title" />
</Alert>

// Test (GOOD)
gcy('signup-error-seats-spending-limit').should('be.visible');

// Test (BAD - don't use text content)
cy.contains('exceeded').should('be.visible');
```

### Error Codes
Backend error codes use `Message.kt` enum, converted to **lowercase** when sent to frontend:
```typescript
cy.intercept('POST', '/v2/projects/*/keys*', {
  statusCode: 400,
  body: {
    code: 'plan_key_limit_exceeded',  // lowercase
    params: [1000, 1001],
  },
}).as('createKey');
```

## Local Startup Troubleshooting

Run the following commands from the platform repository root, which contains `email/` and `webapp/`.

### Backend (BE)

The IDE build failed before the application could launch because Gradle's `:data:buildEmails` task could not find `cross-env`. The error was `cross-env: not found`, and npm exited with code **127**. The email dependencies were missing.

**Fix:** Reinstall the email dependencies:
```bash
npm ci --prefix email
```
Then rerun IntelliJ's **Application** configuration. The backend started on port **8080**, and `http://localhost:8080/actuator/health` returned HTTP 200 with `status: UP`.

### Frontend (FE)

The frontend initially failed with `vite: not found` because its dependencies were missing. Installation then encountered a peer dependency conflict and an out-of-sync lockfile. After installing dependencies, startup failed because the shell used **Node 18**, which was too old for the installed Vite version.

**Fix used in this environment:**

1. Install dependencies using Node 24, without changing the lockfile and with legacy peer dependency resolution:
   ```bash
   npm install --no-package-lock --legacy-peer-deps --prefix webapp
   ```
   Run this with Node 24 available to npm. In this session, the IDE's Node 24 executable was used to invoke npm's CLI directly.
2. Create `webapp/node_modules/.bin/node` as a symlink to the IDE's Node 24 executable so Vite uses that runtime:
   ```bash
   ln -sfn /home/tmy/.cache/JetBrains/IntelliJIdea2026.2/acp-agents/.runtimes/node/24.19.0/bin/node webapp/node_modules/.bin/node
   ```
   This runtime path is specific to this environment. Reinstalling dependencies may remove the symlink; recreate it if needed, or configure Node 24 on PATH.
3. Run IntelliJ's **Frontend** configuration, stored in `.run/Frontend.run.xml`. It runs the `start` script from `webapp` with `VITE_HOST=127.0.0.1`. Alternatively, run:
   ```bash
   npm start --prefix webapp -- --host 127.0.0.1
   ```

The frontend returned HTTP 200. Its default URL is `http://127.0.0.1:3000`; if port 3000 is occupied, Vite selects the next available port and prints the URL in the Run output.

## Git Workflow

### Branch Naming
Format: `firstname-lastname/feature-description`

Generate name from git config:
```bash
git config get user.name | awk '{print $1, $2}' | \
  iconv -f UTF-8 -t ASCII//TRANSLIT | \
  tr -cd '[:alpha:]' | tr '[:upper:]' '[:lower:]'
```

### Commit Message Prefixes
- `feat:` - Breaking changes or new features
- `fix:` - Non-breaking bug fixes
- `chore:` - Non-behavior changes (docs, tests, formatting)

Example: `feat: add CSV export feature`

## Critical Quirks

### Translation Keys
**NEVER** update translation files with new keys manually. Translation keys are automatically added to files after your changes are merged to the main branch. Freely use nonexistent keys in code - they'll be handled outside the codebase.
