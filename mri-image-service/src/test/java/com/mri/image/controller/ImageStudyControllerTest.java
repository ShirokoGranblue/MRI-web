package com.mri.image.controller;

import com.mri.image.config.ImageDemoProperties;
import com.mri.image.model.ImageFile;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.service.ImageStudyService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageStudyControllerTest {
    @Test
    void bindsMultipartSeriesIdAndFileUsingExplicitPartNames() throws Exception {
        ImageStudyService service = mock(ImageStudyService.class);
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        ImageDemoProperties properties = mock(ImageDemoProperties.class);
        ImageStudyController controller = new ImageStudyController(service, repository, properties);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(service.uploadFile(eq(12L), any(MultipartFile.class)))
                .thenReturn(new ImageFile(21L, 12L, "scan.png", "series/12/scan.png", "checksum"));

        mvc.perform(multipart("/images/studies/{studyId}/files", 3L)
                        .file(file)
                        .param("seriesId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(21));
    }
}
