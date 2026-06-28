# Repository Guidelines

## Project Structure & Module Organization
This repository is a Java 21 Spring Boot 3 / Spring Cloud Maven reactor for an MRI image management system. Backend modules are declared in the root `pom.xml`: `mri-common`, `mri-auth-service`, `mri-patient-service`, `mri-exam-service`, `mri-image-service`, `mri-report-service`, and `mri-gateway`. Java sources live in each module at `src/main/java/com/mri/...`; tests live beside them at `src/test/java`. Service configuration is in `src/main/resources/application.yml`. The Vite React frontend is in `mri-frontend`, with UI code in `mri-frontend/src`. SQL bootstrap files are in `docker/mysql/init`, docs in `docs`, runtime verification scripts in `scripts/demo`, cleanup scripts in `scripts/db`, and ignored runtime image storage in `storage/mri-images`.

## Build, Test, and Development Commands
- `docker compose up -d mysql redis nacos minio`: start local infrastructure.
- `mvn -DskipTests install`: compile and install all backend modules.
- `mvn clean test`: run the full backend test suite.
- `mvn -pl mri-auth-service spring-boot:run`: run one backend service; replace the module name as needed.
- `cd mri-frontend; npm install; npm run dev`: install frontend dependencies and start Vite on `localhost:5173`.
- `cd mri-frontend; npm test`: run the lightweight Node frontend regression tests.
- `cd mri-frontend; npm run build`: create the production frontend bundle.
- `powershell -ExecutionPolicy Bypass -File scripts/db/clear-runtime-data.ps1`: clear business data, PATIENT accounts, project Redis data, MinIO objects, and ignored runtime images while preserving admin, system roles, and the bucket.

## Role and Data Ownership
`admin` is presented as the doctor. Doctors can read patient profiles and contraindications and continue to process exam orders, schedules, examinations, image archives, uploads, reports, reviews, and publication. Doctors must not create, edit, or delete patient-owned profiles or contraindications. `PATIENT` users can create and maintain only their profile bound through `patient.account_username`; they can read only their own exam, image, and report progress through `/me` and `/mine` endpoints. Report text and image content remain unavailable to the patient until the report is `PUBLISHED`. Preserve the existing exam and report state machines.

All production frontend traffic goes through gateway port `8080`. Auth and its Swagger use `9001`; patient, exam, image, and report services use `9002` through `9005`; MinIO API uses `9000`, its console uses `9101`, and Nacos uses `8848`. Do not trust client-supplied identity headers. A 401 means the session is invalid and must be cleared; a 403 means the authenticated role lacks permission and the session must remain.

## Coding Style & Naming Conventions
Use 4-space indentation for Java and XML, 2-space indentation for JSX/CSS. Java packages follow `com.mri.<domain>` and classes use clear suffixes already present in the codebase, such as `Controller`, `Service`, `Repository`, `Mapper`, `Entity`, `Request`, and `Response`. Prefer constructor injection for Spring components. React components use PascalCase, helpers use camelCase, imports use single quotes, and shared frontend API helpers belong in `mri-frontend/src/lib`.

## Testing Guidelines
Backend tests use Spring Boot Starter Test with Maven Surefire. Put tests under the same module as the code being covered and name classes `*Test`, for example `AuthServiceTest` or `MybatisPatientRepositoryTest`. Use `mvn -pl <module> test` for a focused module run, and `mvn clean test` before sharing broader backend changes. Frontend regression tests use the existing Node test runner; keep pure session, role, API-path, error-message, and routing logic covered without adding a large browser test framework. Run `npm test` and `npm run build` for frontend changes.

After end-to-end verification, the delivery environment must contain no patient profiles, contraindications, exam orders, schedules, studies, series, image-file records, reports, audit/download records, PATIENT accounts, Redis project keys, MinIO objects, or ignored runtime images. Preserve the enabled `admin` account, all system role definitions including `PATIENT`, required configuration, and the empty MinIO bucket. Final smoke checks must not create a new patient or business record.

## Commit & Pull Request Guidelines
Recent history uses short Conventional Commit-style subjects, for example `feat: add MRI frontend workbench`. Keep commits imperative and scoped: `feat:`, `fix:`, `docs:`, or `test:`. Pull requests should include a concise summary, affected modules, commands run, linked issue or task context, and screenshots for visible frontend changes.

## Security & Configuration Tips
Do not commit secrets, generated logs, `target/`, `node_modules/`, frontend `dist/`, or runtime files under `storage/mri-images`. Keep local service changes in `application.yml` explicit, and document non-default ports or credentials in the PR description. Use the idempotent migration in `docker/mysql/init/02-patient-account-migration.sql` for existing volumes. Keep system Chinese seed values encoded with explicit UTF-8-safe SQL expressions so Windows PowerShell piping cannot corrupt role or display names.
