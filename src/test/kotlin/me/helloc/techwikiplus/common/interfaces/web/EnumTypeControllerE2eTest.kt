package me.helloc.techwikiplus.common.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@E2eTest(generateDocs = true)
class EnumTypeControllerE2eTest : BaseE2eTest() {
    @Test
    fun `GET types review-comments - 리뷰 댓글 타입 목록을 조회할 수 있다`() {
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/types/review-comments")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].value").value("INACCURACY"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("부정확한 내용"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].value").value("NEEDS_IMPROVEMENT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].description").value("개선이 필요한 내용"))
            .andDo(
                documentWithResource(
                    "리뷰 댓글 타입 목록 조회",
                    builder()
                        .tag("Types")
                        .summary("리뷰 댓글 타입 목록 조회")
                        .description(
                            """
                            게시글 개정안에 작성할 수 있는 리뷰 댓글의 타입 목록을 조회합니다.
                            각 타입은 고유한 값(value)과 설명(description)을 가지고 있습니다.

                            **사용 가능한 타입:**
                            - INACCURACY: 부정확한 내용
                            - NEEDS_IMPROVEMENT: 개선이 필요한 내용
                            """.trimIndent(),
                        )
                        .responseSchema(schema("ReviewCommentTypeListResponse"))
                        .responseFields(
                            fieldWithPath("[].value")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 댓글 타입 값"),
                            fieldWithPath("[].description")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 댓글 타입 설명"),
                        )
                        .build(),
                ),
            )
    }
}
