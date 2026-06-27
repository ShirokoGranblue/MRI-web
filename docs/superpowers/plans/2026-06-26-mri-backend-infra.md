# MRI Backend & Infra Implementation Plan (Plan 1 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. NOTE: in this environment subagent/workflow spawning is quota-blocked, so execute inline.

**Goal:** Add MinIO image storage + upload/preview/delete, business-logic guardrails, model field completion, cascade deletes, and real patient exam-history (Feign) — without breaking existing demo scripts or tests.

**Architecture:** Add a `MinioImageStorage` component to mri-image-service (object put/get/remove + bucket ensure); add multipart upload + content-streaming endpoints; enforce status guards on archive/cancel/create-report; complete `ExamOrder`/`Report` model fields; cascade-delete studies/series; wire a new patient→exam Feign link for real exam history. All cross-service calls stay on existing Feign+Nacos pattern (service-to-service, no `/api` prefix).

**Tech Stack:** Java 21, Spring Boot 3.3.13, Spring Cloud 2023.0.6 + Alibaba 2023.0.3.4, MyBatis-Plus 3.5.7, MinIO SDK 8.5.14, MySQL 8, Redis 7, Nacos 2.4.

## Global Constraints

- Feign `@FeignClient` `path` = controller base path WITHOUT `/api` (gateway strips `/api`; service-to-service has no `/api`). E.g. `path = "/exams"`.
- `ApiResult<T>` record: `ok(data)`, `ok()`, `fail(code,msg)`; fields `success/code/message/data`. Feign returns `ApiResult<...>`, callers read `.data()`.
- `BaseEntity` exposes `getCreatedAt()` (`LocalDateTime`).
- Constructor injection everywhere (project convention); update unit-test constructor calls when signatures change.
- Don't break demo scripts: `GET /api/images/studies/1`, `/api/images/studies/1/cache-demo`, `/api/images/demo/config`, `POST /api/images/studies` (JSON) must keep working.
- Multipart upload goes through the gateway (`/api/images/...`), not direct to :9004.
- No schema change to `mri_image_file` (reuse `storage_path` for MinIO object key).

---

### Task B1: MinIO infra + gateway buffer

**Files:**
- Modify: `docker-compose.yml` (add `minio` service + volume)
- Modify: `mri-gateway/src/main/resources/application.yml` (add codec buffer)

- [ ] **Step 1: Add minio service to docker-compose.yml**

Insert after the `nacos:` service block, before `volumes:`:

```yaml
  minio:
    image: minio/minio:latest
    container_name: mri-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: mri
      MINIO_ROOT_PASSWORD: mri123456
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - mri-minio-data:/data
```

Add to the `volumes:` block:

```yaml
  mri-minio-data:
```

- [ ] **Step 2: Add gateway in-memory buffer for large uploads**

In `mri-gateway/src/main/resources/application.yml`, under `spring:` add:

```yaml
  codec:
    max-in-memory-size: 50MB
```

- [ ] **Step 3: Validate compose syntax**

Run: `docker compose config > /dev/null && echo OK`
Expected: `OK` (or compose prints the normalized config without error).

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml mri-gateway/src/main/resources/application.yml
git commit -m "infra: add MinIO service and raise gateway body buffer"
```

---

### Task B2: image-service MinIO dependency, config, storage bean

**Files:**
- Modify: `pom.xml` (root) — add `minio.version` + dependencyManagement
- Modify: `mri-image-service/pom.xml` — add minio dependency
- Modify: `mri-image-service/src/main/resources/application.yml` — add minio + multipart config
- Create: `mri-image-service/src/main/java/com/mri/image/config/MinioProperties.java`
- Create: `mri-image-service/src/main/java/com/mri/image/storage/MinioImageStorage.java`

**Interfaces:**
- Produces: `MinioImageStorage` bean with `putObject(String key, InputStream in, long size, String contentType)`, `LoadedObject loadObject(String key)` (record `LoadedObject(byte[] content, String contentType)`), `removeObjectQuietly(String key)`, and a `@PostConstruct` bucket-ensure.

- [ ] **Step 1: Root pom — add minio version + dependencyManagement**

In `pom.xml` `<properties>` add:

```xml
<minio.version>8.5.14</minio.version>
```

In `<dependencyManagement><dependencies>` add:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>${minio.version}</version>
</dependency>
```

- [ ] **Step 2: image-service pom — add minio dependency**

In `mri-image-service/pom.xml` `<dependencies>` add:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
</dependency>
```

- [ ] **Step 3: application.yml — add minio + multipart config**

In `mri-image-service/src/main/resources/application.yml`, under `mri.image:` add:

```yaml
    minio:
      endpoint: http://localhost:9000
      access-key: mri
      secret-key: mri123456
      bucket: mri-images
```

Under `spring:` add:

```yaml
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

- [ ] **Step 4: Create MinioProperties**

`mri-image-service/src/main/java/com/mri/image/config/MinioProperties.java`:

```java
package com.mri.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mri.image.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket) {
}
```

(`@ConfigurationPropertiesScan` is already on `ImageServiceApplication`.)

- [ ] **Step 5: Create MinioImageStorage**

`mri-image-service/src/main/java/com/mri/image/storage/MinioImageStorage.java`:

```java
package com.mri.image.storage;

import com.mri.image.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
public class MinioImageStorage {
    private static final Logger log = LoggerFactory.getLogger(MinioImageStorage.class);

    public record LoadedObject(byte[] content, String contentType) {
    }

    private final MinioProperties properties;
    private final MinioClient client;

    public MinioImageStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created MinIO bucket {}", properties.bucket());
            }
        } catch (Exception ex) {
            log.warn("MinIO bucket check/create failed (is minio running?): {}", ex.getMessage());
        }
    }

    public void putObject(String objectKey, InputStream in, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("上传影像到对象存储失败: " + ex.getMessage(), ex);
        }
    }

    public LoadedObject loadObject(String objectKey) {
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            String contentType = client.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build()).contentType();
            return new LoadedObject(out.toByteArray(), contentType == null ? "application/octet-stream" : contentType);
        } catch (Exception ex) {
            throw new IllegalStateException("影像文件不存在或未上传: " + objectKey, ex);
        }
    }

    public void removeObjectQuietly(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("删除对象存储对象失败(忽略): {}: {}", objectKey, ex.getMessage());
        }
    }
}
```

- [ ] **Step 6: Verify compile**

Run: `mvn -pl mri-image-service -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add pom.xml mri-image-service/pom.xml mri-image-service/src/main/resources/application.yml mri-image-service/src/main/java/com/mri/image/config/MinioProperties.java mri-image-service/src/main/java/com/mri/image/storage/MinioImageStorage.java
git commit -m "feat(image): add MinIO object storage component and config"
```

---

### Task B3: image-service exam-status Feign + archive guard

**Files:**
- Modify: `mri-image-service/src/main/java/com/mri/image/client/ExamFeignApi.java` — add `status`
- Modify: `mri-image-service/src/main/java/com/mri/image/client/ExamClient.java` — add `examStatus`
- Modify: `mri-image-service/src/main/java/com/mri/image/client/RemoteExamClient.java` — implement `examStatus`
- Modify: `mri-image-service/src/main/java/com/mri/image/service/ImageStudyService.java` — archive guard
- Test: `mri-image-service/src/test/java/com/mri/image/service/ImageStudyServiceTest.java` — update ctor + add archive-guard test

**Interfaces:**
- Consumes: `ExamClient.examStatus(Long)` returns status `String` (null on failure).
- Produces: `archive` throws unless exam status == `COMPLETED`.

- [ ] **Step 1: Write the failing test**

Add to `ImageStudyServiceTest` (and update the 3 existing `new ImageStudyService(repository, cache, examClient)` to `new ImageStudyService(repository, cache, examClient, minioStorage)` with `MinioImageStorage minioStorage = mock(MinioImageStorage.class);`). Add imports `com.mri.image.storage.MinioImageStorage`, `org.springframework.mock.web.MockMultipartFile` not needed here. New test:

```java
@Test
void archiveRequiresCompletedExam() {
    ImageStudyRepository repository = mock(ImageStudyRepository.class);
    StudyCache cache = new StudyCache();
    ExamClient examClient = mock(ExamClient.class);
    MinioImageStorage minioStorage = mock(MinioImageStorage.class);
    ArchiveStudyRequest request = new ArchiveStudyRequest(12L, "STUDY-001", "头颅MRI");
    when(examClient.examExists(12L)).thenReturn(true);
    when(examClient.examStatus(12L)).thenReturn("REQUESTED");

    ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);

    assertThatThrownBy(() -> service.archive(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("完成");
    verify(repository, never()).archive(request);
}
```

Also update the existing `archiveStudyChecksExamBeforeSaving` test to stub `when(examClient.examStatus(12L)).thenReturn("COMPLETED")` so archive still passes.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: compile FAIL (no `examStatus` on `ExamClient`; ctor mismatch).

- [ ] **Step 3: Add examStatus to Feign + client**

`ExamFeignApi.java`:

```java
@GetMapping("/{id}/status")
ApiResult<Map<String, String>> status(@PathVariable("id") Long id);
```

`ExamClient.java`:

```java
String examStatus(Long examOrderId);
```

`RemoteExamClient.java`:

```java
@Override
public String examStatus(Long examOrderId) {
    try {
        return api.status(examOrderId).data().get("status");
    } catch (RuntimeException ex) {
        return null;
    }
}
```

- [ ] **Step 4: Add archive guard to ImageStudyService**

Inject `MinioImageStorage` (4th ctor arg) and guard `archive`:

```java
public MriStudy archive(ArchiveStudyRequest request) {
    if (!examClient.examExists(request.examOrderId())) {
        throw new IllegalArgumentException("检查申请不存在，不能归档 MRI Study");
    }
    String status = examClient.examStatus(request.examOrderId());
    if (!"COMPLETED".equals(status)) {
        throw new IllegalArgumentException("检查申请尚未完成，不能归档影像");
    }
    MriStudy study = repository.archive(request);
    cache.evict(study.id());
    return study;
}
```

Add field `private final MinioImageStorage storage;` and ctor param.

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add mri-image-service/src/main/java/com/mri/image/client/ExamFeignApi.java mri-image-service/src/main/java/com/mri/image/client/ExamClient.java mri-image-service/src/main/java/com/mri/image/client/RemoteExamClient.java mri-image-service/src/main/java/com/mri/image/service/ImageStudyService.java mri-image-service/src/test/java/com/mri/image/service/ImageStudyServiceTest.java
git commit -m "feat(image): require completed exam before archiving study"
```

---

### Task B4: image-service upload + content + delete-object

**Files:**
- Modify: `mri-image-service/src/main/java/com/mri/image/service/ImageStudyService.java` — `uploadFile`, `streamFile`, `deleteFile` removeObject
- Modify: `mri-image-service/src/main/java/com/mri/image/controller/ImageStudyController.java` — multipart upload + content stream endpoints
- Test: `ImageStudyServiceTest` — upload + delete-object tests

**Interfaces:**
- Produces: `ImageStudyService.uploadFile(Long seriesId, MultipartFile file) -> ImageFile`; `streamFile(Long fileId) -> LoadedObject`; `deleteFile` also calls `removeObjectQuietly`.
- Produces endpoints: `POST /api/images/studies/{studyId}/files` (multipart: `seriesId` + `file`); `GET /api/images/files/{id}/content` (byte stream).

- [ ] **Step 1: Write failing tests**

```java
@Test
void uploadFileStoresObjectAndRecordsRow() throws Exception {
    ImageStudyRepository repository = mock(ImageStudyRepository.class);
    StudyCache cache = new StudyCache();
    ExamClient examClient = mock(ExamClient.class);
    MinioImageStorage storage = mock(MinioImageStorage.class);
    MockMultipartFile file = new MockMultipartFile("file", "scan-001.png", "image/png", new byte[]{1, 2, 3});
    when(repository.createFile(any())).thenAnswer(inv -> inv.getArgument(0, ImageFile.class).id() == null ? new ImageFile(7L, 21L, "scan-001.png", "series/21/x-scan-001.png", "abc") : inv.getArgument(0));

    ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
    ImageFile saved = service.uploadFile(21L, file);

    assertThat(saved.seriesId()).isEqualTo(21L);
    assertThat(saved.storagePath()).startsWith("series/21/");
    verify(storage).putObject(org.mockito.ArgumentMatchers.startsWith("series/21/"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq("image/png"));
    verify(repository).createFile(any());
    verify(cache).evict(21L);
}
```
(Also a `deleteFileRemovesObjectThenRow` test: stub `findFile` + `findSeries`, verify `storage.removeObjectQuietly(existing.storagePath())` then `repository.deleteFile(id)`.) Use `cache.evict` — note `StudyCache` must allow `verify(cache).evict(...)`; if `StudyCache` is a concrete class not a mock, either mock it or assert via repository verifications only. Prefer `StudyCache cache = mock(StudyCache.class);` in these two tests (the existing tests use `new StudyCache()`; switching to mock is fine since we only verify interactions).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: FAIL (no `uploadFile`).

- [ ] **Step 3: Implement uploadFile + streamFile + deleteFile in ImageStudyService**

Add imports: `org.springframework.web.multipart.MultipartFile`, `java.security.MessageDigest`, `java.util.UUID`, `com.mri.image.storage.MinioImageStorage.LoadedObject`.

```java
public ImageFile uploadFile(Long seriesId, MultipartFile file) {
    String objectKey = "series/" + seriesId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
    try {
        storage.putObject(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
    } catch (java.io.IOException e) {
        throw new IllegalStateException("读取上传文件失败", e);
    }
    String checksum = sha256(file);
    ImageFile saved = repository.createFile(new ImageFile(null, seriesId, file.getOriginalFilename(), objectKey, checksum));
    cache.evict(seriesStudyId(seriesId));
    return saved;
}

public LoadedObject streamFile(Long fileId) {
    ImageFile file = repository.findFile(fileId)
            .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
    return storage.loadObject(file.storagePath());
}

@Override
public void deleteFile(Long id) {
    ImageFile existing = repository.findFile(id)
            .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
    MriSeries series = repository.findSeries(existing.seriesId())
            .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
    storage.removeObjectQuietly(existing.storagePath());
    repository.deleteFile(id);
    cache.evict(series.studyId());
}

private String sha256(MultipartFile file) {
    try {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    } catch (Exception e) {
        return null;
    }
}

private Long seriesStudyId(Long seriesId) {
    return repository.findSeries(seriesId).map(MriSeries::studyId).orElse(null);
}
```

(`deleteFile` above replaces the existing one; keep `createFile` metadata method for the JSON endpoint.)

- [ ] **Step 4: Add controller endpoints**

In `ImageStudyController`, add imports `org.springframework.web.multipart.MultipartFile`, `org.springframework.http.ResponseEntity`, `org.springframework.http.MediaType`, `org.springframework.web.bind.annotation.RequestPart`, `com.mri.image.storage.MinioImageStorage.LoadedObject`. Add `consumes = MediaType.APPLICATION_JSON_VALUE` to the existing `uploadFile` (JSON) `@PostMapping`. Add:

```java
@Operation(summary = "上传影像文件到对象存储")
@PostMapping(value = "/studies/{studyId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResult<ImageFile> uploadToStorage(@PathVariable Long studyId,
                                            @RequestParam Long seriesId,
                                            @RequestPart MultipartFile file) {
    return ApiResult.ok(service.uploadFile(seriesId, file));
}

@Operation(summary = "影像文件内容")
@GetMapping("/files/{id}/content")
public ResponseEntity<byte[]> fileContent(@PathVariable Long id) {
    LoadedObject obj = service.streamFile(id);
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(obj.contentType()))
            .body(obj.content());
}
```

- [ ] **Step 5: Run tests to verify pass**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add mri-image-service/src/main/java/com/mri/image/service/ImageStudyService.java mri-image-service/src/main/java/com/mri/image/controller/ImageStudyController.java mri-image-service/src/test/java/com/mri/image/service/ImageStudyServiceTest.java
git commit -m "feat(image): upload images to MinIO and stream content"
```

---

### Task B5: image-service cascade delete (study/series)

**Files:**
- Modify: `mri-image-service/src/main/java/com/mri/image/repository/ImageStudyRepository.java` — add `findFilesBySeriesId`, `deleteFilesByStudyId`, `deleteFilesBySeriesId`, `deleteSeriesByStudyId`
- Modify: `mri-image-service/src/main/java/com/mri/image/repository/MybatisImageStudyRepository.java` — implement them
- Modify: `ImageStudyService.java` — cascade in `deleteStudy` + `deleteSeries`
- Test: `ImageStudyServiceTest` — cascade test

- [ ] **Step 1: Write failing test**

```java
@Test
void deleteStudyCascadesSeriesFilesAndObjects() {
    ImageStudyRepository repository = mock(ImageStudyRepository.class);
    StudyCache cache = mock(StudyCache.class);
    ExamClient examClient = mock(ExamClient.class);
    MinioImageStorage storage = mock(MinioImageStorage.class);
    when(repository.findFilesByStudyId(5L)).thenReturn(java.util.List.of(
            new ImageFile(101L, 21L, "a.png", "series/21/a.png", "x"),
            new ImageFile(102L, 22L, "b.png", "series/22/b.png", "y")));

    ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
    service.deleteStudy(5L);

    verify(storage).removeObjectQuietly("series/21/a.png");
    verify(storage).removeObjectQuietly("series/22/b.png");
    verify(repository).deleteFilesByStudyId(5L);
    verify(repository).deleteSeriesByStudyId(5L);
    verify(repository).deleteStudy(5L);
    verify(cache).evict(5L);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: FAIL (no `deleteFilesByStudyId`).

- [ ] **Step 3: Add repository methods**

`ImageStudyRepository.java`:

```java
List<ImageFile> findFilesBySeriesId(Long seriesId);
void deleteFilesByStudyId(Long studyId);
void deleteFilesBySeriesId(Long seriesId);
void deleteSeriesByStudyId(Long studyId);
```

`MybatisImageStudyRepository.java`:

```java
@Override
public List<ImageFile> findFilesBySeriesId(Long seriesId) {
    return fileMapper.selectList(new LambdaQueryWrapper<ImageFileEntity>().eq(ImageFileEntity::getSeriesId, seriesId))
            .stream().map(MybatisImageStudyRepository::toModel).toList();
}

@Override
public void deleteFilesByStudyId(Long studyId) {
    Set<Long> seriesIds = findSeriesByStudyId(studyId).stream().map(MriSeries::id).collect(java.util.stream.Collectors.toSet());
    if (seriesIds.isEmpty()) return;
    fileMapper.delete(new LambdaQueryWrapper<ImageFileEntity>().in(ImageFileEntity::getSeriesId, seriesIds));
}

@Override
public void deleteFilesBySeriesId(Long seriesId) {
    fileMapper.delete(new LambdaQueryWrapper<ImageFileEntity>().eq(ImageFileEntity::getSeriesId, seriesId));
}

@Override
public void deleteSeriesByStudyId(Long studyId) {
    seriesMapper.delete(new LambdaQueryWrapper<MriSeriesEntity>().eq(MriSeriesEntity::getStudyId, studyId));
}
```

- [ ] **Step 4: Cascade in ImageStudyService**

```java
@Override
public void deleteStudy(Long id) {
    for (ImageFile file : repository.findFilesByStudyId(id)) {
        storage.removeObjectQuietly(file.storagePath());
    }
    repository.deleteFilesByStudyId(id);
    repository.deleteSeriesByStudyId(id);
    repository.deleteStudy(id);
    cache.evict(id);
}

@Override
public void deleteSeries(Long id) {
    MriSeries existing = repository.findSeries(id)
            .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
    for (ImageFile file : repository.findFilesBySeriesId(id)) {
        storage.removeObjectQuietly(file.storagePath());
    }
    repository.deleteFilesBySeriesId(id);
    repository.deleteSeries(id);
    cache.evict(existing.studyId());
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -pl mri-image-service -am test -Dtest=ImageStudyServiceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add mri-image-service/src/main/java/com/mri/image/repository/ImageStudyRepository.java mri-image-service/src/main/java/com/mri/image/repository/MybatisImageStudyRepository.java mri-image-service/src/main/java/com/mri/image/service/ImageStudyService.java mri-image-service/src/test/java/com/mri/image/service/ImageStudyServiceTest.java
git commit -m "feat(image): cascade delete study/series with object cleanup"
```

---

### Task B6: exam-service model completion + cancel guard + by-patient

**Files:**
- Modify: `mri-exam-service/src/main/java/com/mri/exam/model/ExamOrder.java` — add `clinicalDiagnosis`, `priority`, `createdAt`
- Modify: `mri-exam-service/src/main/java/com/mri/exam/repository/MybatisExamOrderRepository.java` — `toModel` + add `listByPatient`
- Modify: `mri-exam-service/src/main/java/com/mri/exam/repository/ExamOrderRepository.java` — add `listByPatient`
- Modify: `mri-exam-service/src/main/java/com/mri/exam/service/ExamOrderService.java` — `cancel` guard + `listByPatient`
- Modify: `mri-exam-service/src/main/java/com/mri/exam/controller/ExamOrderController.java` — `GET /by-patient/{patientId}`
- Test: `mri-exam-service/src/test/java/com/mri/exam/service/ExamOrderServiceTest.java` — update 3 ctor usages + cancel-guard test

**Interfaces:**
- Produces: `ExamOrder(id, patientId, examItem, clinicalDiagnosis, priority, status, createdAt)`; `GET /api/exams/by-patient/{patientId} -> List<ExamOrder>`; `cancel` rejects unless REQUESTED/IN_PROGRESS.

- [ ] **Step 1: Write failing test**

Update the 3 existing `new ExamOrder(11L, 3L, "头颅MRI平扫", "REQUESTED")` to `new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)`. Add:

```java
@Test
void cancelRejectsCompletedExam() {
    PatientClient patientClient = mock(PatientClient.class);
    ExamOrderRepository repository = mock(ExamOrderRepository.class);
    when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "COMPLETED", null)));

    ExamOrderService service = new ExamOrderService(repository, patientClient);

    assertThatThrownBy(() -> service.cancel(11L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("取消");
    verify(repository, never()).cancel(11L);
}
```

Add `import static org.mockito.ArgumentMatchers.*;` if needed.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-exam-service -am test -Dtest=ExamOrderServiceTest`
Expected: FAIL (ctor arity, no cancel guard).

- [ ] **Step 3: Update ExamOrder model**

`ExamOrder.java`:

```java
package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "MRI 检查申请单")
public record ExamOrder(Long id, Long patientId, String examItem, String clinicalDiagnosis,
                        String priority, String status, LocalDateTime createdAt) {
}
```

- [ ] **Step 4: Update toModel + add listByPatient (repository)**

`MybatisExamOrderRepository.toModel`:

```java
private static ExamOrder toModel(ExamOrderEntity entity) {
    return new ExamOrder(entity.getId(), entity.getPatientId(), entity.getExamItem(),
            entity.getClinicalDiagnosis(), entity.getPriority(), entity.getStatus(), entity.getCreatedAt());
}
```

`ExamOrderRepository` add:

```java
java.util.List<ExamOrder> listByPatient(Long patientId);
```

`MybatisExamOrderRepository` implement:

```java
@Override
public java.util.List<ExamOrder> listByPatient(Long patientId) {
    return examOrderMapper.selectList(new LambdaQueryWrapper<ExamOrderEntity>()
                    .eq(ExamOrderEntity::getPatientId, patientId)
                    .orderByDesc(ExamOrderEntity::getId))
            .stream().map(MybatisExamOrderRepository::toModel).toList();
}
```

(Verify `ExamOrderEntity` has `clinicalDiagnosis`/`priority` getters — it should per schema. If not, add them.)

- [ ] **Step 5: cancel guard + listByPatient in service**

`ExamOrderService`:

```java
public void cancel(Long id) {
    ExamOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
    if (!java.util.Set.of("REQUESTED", "IN_PROGRESS").contains(order.status())) {
        throw new IllegalArgumentException("仅待检查或进行中的检查可取消，当前状态为 " + order.status());
    }
    repository.cancel(id);
}

public java.util.List<ExamOrder> listByPatient(Long patientId) {
    return repository.listByPatient(patientId);
}
```

- [ ] **Step 6: Controller endpoint**

`ExamOrderController`:

```java
@Operation(summary = "按患者查询检查申请")
@GetMapping("/by-patient/{patientId}")
public ApiResult<java.util.List<ExamOrder>> byPatient(@PathVariable Long patientId) {
    return ApiResult.ok(service.listByPatient(patientId));
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -pl mri-exam-service -am test -Dtest=ExamOrderServiceTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add mri-exam-service/src/main/java/com/mri/exam/model/ExamOrder.java mri-exam-service/src/main/java/com/mri/exam/repository/ExamOrderRepository.java mri-exam-service/src/main/java/com/mri/exam/repository/MybatisExamOrderRepository.java mri-exam-service/src/main/java/com/mri/exam/service/ExamOrderService.java mri-exam-service/src/main/java/com/mri/exam/controller/ExamOrderController.java mri-exam-service/src/test/java/com/mri/exam/service/ExamOrderServiceTest.java
git commit -m "feat(exam): complete model, cancel guard, by-patient query"
```

---

### Task B7: report-service model completion + create guard + reopen

**Files:**
- Modify: `mri-report-service/src/main/java/com/mri/report/model/Report.java` — add `impression`
- Modify: `mri-report-service/src/main/java/com/mri/report/repository/MybatisReportRepository.java` — `toModel`
- Modify: `mri-report-service/src/main/java/com/mri/report/client/ExamFeignApi.java` — add `status`
- Modify: `mri-report-service/src/main/java/com/mri/report/client/ExamClient.java` — add `examStatus`
- Modify: `mri-report-service/src/main/java/com/mri/report/client/RemoteExamClient.java` — impl `examStatus`
- Modify: `mri-report-service/src/main/java/com/mri/report/client/ImageClient.java` — add `studyExists`
- Modify: `mri-report-service/src/main/java/com/mri/report/client/RemoteImageClient.java` — impl `studyExists`
- Modify: `mri-report-service/src/main/java/com/mri/report/service/ReportService.java` — `create` guard + `reopen`
- Modify: `mri-report-service/src/main/java/com/mri/report/controller/ReportController.java` — `POST /{id}/reopen`
- Test: `ReportServiceTest` — update 5 ctor usages + create-guard + reopen tests

**Interfaces:**
- Produces: `Report(id, examOrderId, studyId, findings, impression, status)`; `create` requires exam COMPLETED + study exists; `reopen(id)` REJECTED→DRAFT; `POST /api/reports/{id}/reopen`.

- [ ] **Step 1: Write failing tests**

Update existing `new Report(31L, 12L, 5L, "...", "STATUS")` → `new Report(31L, 12L, 5L, "...", null, "STATUS")`. Update `createReportStartsAsDraft` to stub guards: `when(examClient.examStatus(12L)).thenReturn("COMPLETED"); when(imageClient.studyExists(5L)).thenReturn(true);`. Add:

```java
@Test
void createRejectsUnlessExamCompletedAndStudyExists() {
    ReportRepository repository = mock(ReportRepository.class);
    ImageClient imageClient = mock(ImageClient.class);
    ExamClient examClient = mock(ExamClient.class);
    CreateReportRequest request = new CreateReportRequest(12L, 5L, "所见", "意见");
    when(examClient.examStatus(12L)).thenReturn("IN_PROGRESS");

    ReportService service = new ReportService(repository, imageClient, examClient);
    assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("完成");
    verify(repository, never()).createDraft(request);
}

@Test
void reopenReturnsRejectedToDraft() {
    ReportRepository repository = mock(ReportRepository.class);
    ImageClient imageClient = mock(ImageClient.class);
    ExamClient examClient = mock(ExamClient.class);
    when(repository.findById(31L)).thenReturn(Optional.of(new Report(31L, 12L, 5L, "所见", "意见", "REJECTED")));
    when(repository.updateStatus(31L, "DRAFT")).thenReturn(new Report(31L, 12L, 5L, "所见", "意见", "DRAFT"));

    ReportService service = new ReportService(repository, imageClient, examClient);
    assertThat(service.reopen(31L).status()).isEqualTo("DRAFT");
    verify(repository).audit(31L, "REOPEN", "diagnosis-doctor", "回到草稿修改");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-report-service -am test -Dtest=ReportServiceTest`
Expected: FAIL (no `reopen`, no `examStatus`).

- [ ] **Step 3: Update Report model + toModel**

`Report.java`:

```java
public record Report(Long id, Long examOrderId, Long studyId, String findings, String impression, String status) {
}
```

`MybatisReportRepository.toModel`:

```java
return new Report(entity.getId(), entity.getExamOrderId(), entity.getStudyId(), entity.getFindings(), entity.getImpression(), entity.getStatus());
```

(Verify `ReportEntity` has `getImpression()`; if not, add field+getter matching the `impression` column.)

- [ ] **Step 4: Add Feign methods**

`ExamFeignApi` (report): add `@GetMapping("/{id}/status") ApiResult<Map<String, String>> status(@PathVariable("id") Long id);` + import `java.util.Map`, `GetMapping`.
`ExamClient`: add `String examStatus(Long examOrderId);`
`RemoteExamClient`:

```java
@Override
public String examStatus(Long examOrderId) {
    try { return api.status(examOrderId).data().get("status"); }
    catch (RuntimeException ex) { return null; }
}
```

`ImageClient`: add `boolean studyExists(Long studyId);`
`RemoteImageClient`:

```java
@Override
public boolean studyExists(Long studyId) {
    try { api.study(studyId); return true; }
    catch (RuntimeException ex) { return false; }
}
```

- [ ] **Step 5: create guard + reopen in ReportService**

```java
public Report create(CreateReportRequest request) {
    String examStatus = examClient.examStatus(request.examOrderId());
    if (!"COMPLETED".equals(examStatus)) {
        throw new IllegalArgumentException("检查尚未完成，不能创建报告");
    }
    if (!imageClient.studyExists(request.studyId())) {
        throw new IllegalArgumentException("对应影像未归档，不能创建报告");
    }
    return repository.createDraft(request);
}

public Report reopen(Long id) {
    Report report = requireReport(id);
    requireStatus(report, "REJECTED");
    Report draft = repository.updateStatus(id, "DRAFT");
    repository.audit(id, "REOPEN", "diagnosis-doctor", "回到草稿修改");
    return draft;
}
```

- [ ] **Step 6: Controller endpoint**

`ReportController`:

```java
@Operation(summary = "回到草稿修改")
@PostMapping("/{id}/reopen")
public ApiResult<Report> reopen(@PathVariable Long id) {
    return ApiResult.ok(service.reopen(id));
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -pl mri-report-service -am test -Dtest=ReportServiceTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add mri-report-service/src/main/java/com/mri/report/model/Report.java mri-report-service/src/main/java/com/mri/report/repository/MybatisReportRepository.java mri-report-service/src/main/java/com/mri/report/client/ mri-report-service/src/main/java/com/mri/report/service/ReportService.java mri-report-service/src/main/java/com/mri/report/controller/ReportController.java mri-report-service/src/test/java/com/mri/report/service/ReportServiceTest.java
git commit -m "feat(report): complete model, create guard, reopen rejected report"
```

---

### Task B8: patient-service real exam-history via Feign

**Files:**
- Modify: `mri-patient-service/pom.xml` — add openfeign + loadbalancer
- Modify: `mri-patient-service/src/main/java/com/mri/patient/PatientServiceApplication.java` — `@EnableFeignClients`
- Create: `mri-patient-service/src/main/java/com/mri/patient/client/ExamClient.java`
- Create: `mri-patient-service/src/main/java/com/mri/patient/client/ExamFeignApi.java`
- Create: `mri-patient-service/src/main/java/com/mri/patient/client/RemoteExamClient.java`
- Modify: `mri-patient-service/src/main/java/com/mri/patient/service/PatientService.java` — inject `ExamClient`, `examHistory(patientId)`
- Modify: `mri-patient-service/src/main/java/com/mri/patient/controller/PatientController.java` — `examHistory` calls service
- Modify: `mri-patient-service/src/main/java/com/mri/patient/repository/MybatisPatientRepository.java` — drop stub (or leave unused)
- Test: `mri-patient-service/src/test/java/com/mri/patient/service/PatientServiceTest.java` (create if absent)

**Interfaces:**
- Produces: `PatientService.examHistory(Long patientId) -> List<PatientExamHistory>` calling exam-service `GET /exams/by-patient/{patientId}` via Feign; empty list on failure.

- [ ] **Step 1: Write failing test**

Create/extend `mri-patient-service/src/test/java/com/mri/patient/service/PatientServiceTest.java`:

```java
package com.mri.patient.service;

import com.mri.patient.client.ExamClient;
import com.mri.patient.model.PatientExamHistory;
import com.mri.patient.repository.PatientRepository;
import com.mri.patient.service.PatientCache;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PatientServiceTest {
    @Test
    void examHistoryMapsRemoteExams() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = mock(PatientCache.class);
        ExamClient examClient = mock(ExamClient.class);
        when(examClient.listByPatient(3L)).thenReturn(List.of(
                new com.mri.patient.client.ExamSummary(3L, "头颅MRI平扫", "COMPLETED", null)));
        PatientService service = new PatientService(repository, cache, examClient);

        List<PatientExamHistory> history = service.examHistory(3L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).examItem()).isEqualTo("头颅MRI平扫");
        assertThat(history.get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    void examHistoryReturnsEmptyWhenRemoteFails() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = mock(PatientCache.class);
        ExamClient examClient = mock(ExamClient.class);
        when(examClient.listByPatient(3L)).thenReturn(List.of());
        PatientService service = new PatientService(repository, cache, examClient);
        assertThat(service.examHistory(3L)).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl mri-patient-service -am test -Dtest=PatientServiceTest`
Expected: FAIL (no ExamClient / ctor).

- [ ] **Step 3: pom + @EnableFeignClients**

`mri-patient-service/pom.xml` add:

```xml
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-loadbalancer</artifactId></dependency>
```

`PatientServiceApplication.java` add `@EnableFeignClients` + import.

- [ ] **Step 4: Create ExamClient + ExamFeignApi + RemoteExamClient**

`client/ExamSummary.java`:

```java
package com.mri.patient.client;
import java.time.LocalDateTime;
public record ExamSummary(Long patientId, String examItem, String status, LocalDateTime createdAt) {}
```

`client/ExamFeignApi.java`:

```java
package com.mri.patient.client;
import com.mri.common.api.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
@FeignClient(name = "mri-exam-service", path = "/exams")
public interface ExamFeignApi {
    @GetMapping("/by-patient/{patientId}")
    ApiResult<List<ExamSummary>> byPatient(@PathVariable("patientId") Long patientId);
}
```

`client/ExamClient.java`:

```java
package com.mri.patient.client;
import java.util.List;
public interface ExamClient {
    List<ExamSummary> listByPatient(Long patientId);
}
```

`client/RemoteExamClient.java`:

```java
package com.mri.patient.client;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class RemoteExamClient implements ExamClient {
    private final ExamFeignApi api;
    public RemoteExamClient(ExamFeignApi api) { this.api = api; }
    @Override
    public List<ExamSummary> listByPatient(Long patientId) {
        try { return api.byPatient(patientId).data(); }
        catch (RuntimeException ex) { return List.of(); }
    }
}
```

Note: `ExamSummary` field order must match the JSON returned by exam-service `ExamOrder(id, patientId, examItem, clinicalDiagnosis, priority, status, createdAt)`. Feign deserializes by field NAME, not order, so this is fine — `ExamSummary` only declares the fields it cares about (patientId, examItem, status, createdAt); others are ignored. ✓

- [ ] **Step 5: PatientService.examHistory + controller**

`PatientService` — inject `ExamClient`, add:

```java
public java.util.List<com.mri.patient.model.PatientExamHistory> examHistory(Long patientId) {
    return examClient.listByPatient(patientId).stream()
            .map(e -> new com.mri.patient.model.PatientExamHistory(e.patientId(), e.examItem(), e.status(), e.createdAt()))
            .toList();
}
```

Add `private final ExamClient examClient;` + ctor param (3rd).

`PatientController.examHistory` — change `repository.examHistory(patientId)` → `patientService.examHistory(patientId)`.

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -pl mri-patient-service -am test -Dtest=PatientServiceTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add mri-patient-service/pom.xml mri-patient-service/src/main/java/com/mri/patient/PatientServiceApplication.java mri-patient-service/src/main/java/com/mri/patient/client/ mri-patient-service/src/main/java/com/mri/patient/service/PatientService.java mri-patient-service/src/main/java/com/mri/patient/controller/PatientController.java mri-patient-service/src/test/java/com/mri/patient/service/PatientServiceTest.java
git commit -m "feat(patient): real exam history via exam-service Feign"
```

---

### Task B9: Full backend build + test

- [ ] **Step 1: Full build + test**

Run: `mvn clean test`
Expected: BUILD SUCCESS, all modules green, test count = prior 16 + new (≈8). Note actual count.

- [ ] **Step 2: If failures, fix and re-run until green.**

- [ ] **Step 3: Commit any fix-ups**

```bash
git add -A
git commit -m "test: backend green after image/exam/report/patient changes"
```

---

## Self-Review (run after writing, fix inline)

- Spec coverage: A1 archive guard (B3), A2 create-report guard (B7), A3 cancel guard (B6), A4 reopen (B7), A5 frontend status-disable (frontend plan), B6/B7 model completion (B6/B7), B8 cascade (B5), C9 frontend dependency prompt (frontend plan), D11-D16 (frontend plan), D20 edit (frontend plan; backend PUT already exists), E17 real exam-history (B8), E18 contraindication warning (frontend plan), E19 no-delete-published (frontend plan). MinIO infra (B1), storage (B2), upload/content (B4). ✓ all backend items have a task.
- Placeholder scan: none.
- Type consistency: `ExamOrder` 7-arg everywhere; `Report` 6-arg; `MinioImageStorage.LoadedObject` used in service + controller; `ExamClient.examStatus` consistent across image+report; `ExamSummary` fields match ExamOrder JSON by name. ✓

## Execution Handoff

Plan 1 complete. Execute inline (subagent/workflow quota-blocked in this env). After B9 green, write Plan 2 (Frontend rewrite) then Plan 3 (Docs + verification).
