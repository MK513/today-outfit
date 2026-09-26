package com.todayoutfit.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.todayoutfit.IntegrationTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
@Import(ErrorAndPagingTest.TestController.class)
class ErrorAndPagingTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiExceptionUsesErrorSchema() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("삭제되었거나 존재하지 않는 코디입니다."));
    }

    @Test
    void validationFailureReturnsFieldMessage() throws Exception {
        mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{\"display_name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("이름을 입력해주세요."));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"));
    }

    @Test
    void pageIsOneIndexedAndSnakeCase() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.total_elements").value(12))
                .andExpect(jsonPath("$.total_pages").value(3))
                .andExpect(jsonPath("$.content[0]").value("item-5"));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(get("/test/page").param("size", "500"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(100));
    }

    record NameRequest(@NotBlank(message = "이름을 입력해주세요.") String displayName) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw ApiException.notFound("삭제되었거나 존재하지 않는 코디입니다.");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody NameRequest request) {
            return request.displayName();
        }

        @GetMapping("/test/page")
        PageResponse<String> page(Pageable pageable) {
            List<String> all = java.util.stream.IntStream.range(0, 12).mapToObj(i -> "item-" + i).toList();
            int from = (int) Math.min(pageable.getOffset(), all.size());
            int to = Math.min(from + pageable.getPageSize(), all.size());
            return PageResponse.from(new PageImpl<>(all.subList(from, to), pageable, all.size()), s -> s);
        }
    }
}
