package com.placeboard.place;

import com.placeboard.common.GlobalExceptionHandler;
import com.placeboard.map.MapProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceControllerTest {
    private PlacePreviewService placePreviewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        placePreviewService = mock(PlacePreviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaceController(placePreviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPlacePreview() throws Exception {
        String url = "https://naver.me/GXApFSiE";
        when(placePreviewService.preview(url))
                .thenReturn(PlacePreviewResponse.of(MapProviderType.NAVER, "1610498965", url, "티원카페&아레나",
                        "서울 강남구 선릉로115길 4", null, null, null, "카페", null));

        mockMvc.perform(post("/api/places/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://naver.me/GXApFSiE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("NAVER"))
                .andExpect(jsonPath("$.providerPlaceId").value("1610498965"))
                .andExpect(jsonPath("$.name").value("티원카페&아레나"))
                .andExpect(jsonPath("$.sido").value("서울특별시"));
    }

    @Test
    void rejectsBlankUrl() throws Exception {
        mockMvc.perform(post("/api/places/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
