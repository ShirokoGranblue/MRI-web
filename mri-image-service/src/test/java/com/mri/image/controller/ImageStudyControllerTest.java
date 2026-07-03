package com.mri.image.controller;

import com.mri.common.api.PageResult;
import com.mri.image.config.ImageDemoProperties;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.service.ImageDownloadService;
import com.mri.image.service.ImageStudyService;
import com.mri.image.service.PatientImageQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageStudyControllerTest {
    @Test
    void viewerManifestBindsStudyIdWithoutCompilerParameterMetadata() throws Exception {
        ImageStudyService service = mock(ImageStudyService.class);
        ImageDemoProperties properties = mock(ImageDemoProperties.class);
        ImageStudyController controller = new ImageStudyController(
                service,
                mock(ImageStudyRepository.class),
                properties,
                mock(PatientImageQueryService.class),
                mock(ImageDownloadService.class)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/images/studies/{studyId}/viewer-manifest", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).viewerManifest(2L, null, false);
    }

    @Test
    void downloadLogsBindStudyIdWithoutCompilerParameterMetadata() throws Exception {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        when(repository.downloadLogs(2L)).thenReturn(List.of());
        ImageStudyController controller = new ImageStudyController(
                mock(ImageStudyService.class),
                repository,
                mock(ImageDemoProperties.class),
                mock(PatientImageQueryService.class),
                mock(ImageDownloadService.class)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/images/studies/{studyId}/download-logs", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(repository).downloadLogs(2L);
    }

    @Test
    void pageStudiesBindsRequestParametersWithoutCompilerParameterMetadata() throws Exception {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        when(repository.pageStudies(1L, 10L, null))
                .thenReturn(PageResult.of(1L, 10L, 0L, List.of()));
        ImageStudyController controller = new ImageStudyController(
                mock(ImageStudyService.class),
                repository,
                mock(ImageDemoProperties.class),
                mock(PatientImageQueryService.class),
                mock(ImageDownloadService.class)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/images/studies")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(repository).pageStudies(1L, 10L, null);
    }

    @Test
    void createSeriesBindsStudyIdWithoutCompilerParameterMetadata() throws Exception {
        ImageStudyService service = mock(ImageStudyService.class);
        ImageStudyController controller = new ImageStudyController(
                service,
                mock(ImageStudyRepository.class),
                mock(ImageDemoProperties.class),
                mock(PatientImageQueryService.class),
                mock(ImageDownloadService.class)
        );
        when(service.createSeries(any(MriSeries.class)))
                .thenReturn(new MriSeries(8L, 3L, "AX T1", "HEAD"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/images/studies/{studyId}/series", 3L)
                        .contentType("application/json")
                        .content("""
                                {"seriesName":"AX T1","bodyPosition":"HEAD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studyId").value(3));
    }

    @Test
    void doctorSingleDownloadUsesTrustedUsernameAndReturnsAttachment() throws Exception {
        ImageStudyService service = mock(ImageStudyService.class);
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        ImageDemoProperties properties = mock(ImageDemoProperties.class);
        ImageDownloadService downloadService = mock(ImageDownloadService.class);
        when(downloadService.downloadDoctorFile(41L, "admin", "会诊"))
                .thenReturn(new ImageDownloadService.DownloadedFile("扫描-01.png", "image/png", new byte[]{1, 2, 3}));
        ImageStudyController controller = new ImageStudyController(
                service,
                repository,
                properties,
                mock(PatientImageQueryService.class),
                downloadService
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/images/files/{fileId}/download", 41L)
                        .header("X-Authenticated-User", "admin")
                        .param("operator", "attacker")
                        .param("reason", "会诊"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("UTF-8")));

        verify(downloadService).downloadDoctorFile(41L, "admin", "会诊");
    }

    @Test
    void browserStudyDownloadAvoidsAttachmentHeadersInterceptedByDownloadManagers() throws Exception {
        ImageDownloadService downloadService = mock(ImageDownloadService.class);
        when(downloadService.downloadDoctorStudy(31L, "admin", "会诊"))
                .thenReturn(new ImageDownloadService.DownloadArchive(
                        "Study-31-影像.zip",
                        output -> output.write(new byte[]{1, 2, 3})
                ));
        ImageStudyController controller = new ImageStudyController(
                mock(ImageStudyService.class),
                mock(ImageStudyRepository.class),
                mock(ImageDemoProperties.class),
                mock(PatientImageQueryService.class),
                downloadService
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MvcResult result = mvc.perform(get("/images/studies/{studyId}/download", 31L)
                        .header("X-Authenticated-User", "admin")
                        .param("reason", "会诊")
                        .param("transport", "browser"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(content().contentType("application/vnd.mri.study-archive"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void bindsMultipartSeriesIdAndFileUsingExplicitPartNames() throws Exception {
        ImageStudyService service = mock(ImageStudyService.class);
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        ImageDemoProperties properties = mock(ImageDemoProperties.class);
        ImageStudyController controller = new ImageStudyController(
                service,
                repository,
                properties,
                mock(PatientImageQueryService.class),
                mock(ImageDownloadService.class)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(service.uploadFile(eq(3L), eq(12L), any(MultipartFile.class)))
                .thenReturn(new ImageFile(21L, 12L, "scan.png", "series/12/scan.png", "checksum"));

        mvc.perform(multipart("/images/studies/{studyId}/files", 3L)
                        .file(file)
                        .param("seriesId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(21));
    }
}
