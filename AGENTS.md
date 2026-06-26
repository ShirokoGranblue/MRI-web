# Repository Guidelines

## Project Structure & Module Organization
This repository is a Java 21 Spring Boot 3 / Spring Cloud Maven reactor for an MRI image management demo. Backend modules are declared in the root `pom.xml`: `mri-common`, `mri-auth-service`, `mri-patient-service`, `mri-exam-service`, `mri-image-service`, `mri-report-service`, and `mri-gateway`. Java sources live in each module at `src/main/java/com/mri/...`; tests live beside them at `src/test/java`. Service configuration is in `src/main/resources/application.yml`. The Vite React frontend is in `mri-frontend`, with UI code in `mri-frontend/src`. SQL bootstrap files are in `docker/mysql/init`, docs in `docs`, demo scripts in `scripts/demo`, and ignored runtime image storage in `storage/mri-images`.

## Build, Test, and Development Commands
- `docker compose up -d mysql redis nacos`: start local infrastructure.
- `mvn -DskipTests install`: compile and install all backend modules.
- `mvn clean test`: run the full backend test suite.
- `mvn -pl mri-auth-service spring-boot:run`: run one backend service; replace the module name as needed.
- `cd mri-frontend; npm install; npm run dev`: install frontend dependencies and start Vite on `localhost:5173`.
- `cd mri-frontend; npm run build`: create the production frontend bundle.

## Coding Style & Naming Conventions
Use 4-space indentation for Java and XML, 2-space indentation for JSX/CSS. Java packages follow `com.mri.<domain>` and classes use clear suffixes already present in the codebase, such as `Controller`, `Service`, `Repository`, `Mapper`, `Entity`, `Request`, and `Response`. Prefer constructor injection for Spring components. React components use PascalCase, helpers use camelCase, imports use single quotes, and shared frontend API helpers belong in `mri-frontend/src/lib`.

## Testing Guidelines
Backend tests use Spring Boot Starter Test with Maven Surefire. Put tests under the same module as the code being covered and name classes `*Test`, for example `AuthServiceTest` or `MybatisPatientRepositoryTest`. Use `mvn -pl <module> test` for a focused module run, and `mvn clean test` before sharing broader backend changes. The frontend currently has no `npm test` script; add one alongside any new frontend test framework.

## Commit & Pull Request Guidelines
Recent history uses short Conventional Commit-style subjects, for example `feat: add MRI frontend workbench`. Keep commits imperative and scoped: `feat:`, `fix:`, `docs:`, or `test:`. Pull requests should include a concise summary, affected modules, commands run, linked issue or task context, and screenshots for visible frontend changes.

## Security & Configuration Tips
Do not commit secrets, generated logs, `target/`, `node_modules/`, frontend `dist/`, or runtime files under `storage/mri-images`. Keep local service changes in `application.yml` explicit, and document non-default ports or credentials in the PR description.
