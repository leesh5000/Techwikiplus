package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class PostErrorCodeMapper {
    fun mapToHttpStatus(postErrorCode: PostErrorCode): HttpStatus {
        return when (postErrorCode) {
            // Post Status
            PostErrorCode.POST_NOT_FOUND -> HttpStatus.NOT_FOUND
            PostErrorCode.POST_DELETED -> HttpStatus.GONE
            PostErrorCode.POST_DRAFT,
            PostErrorCode.POST_IN_REVIEW,
            -> HttpStatus.FORBIDDEN

            // Post Management
            PostErrorCode.DUPLICATE_TITLE -> HttpStatus.CONFLICT
            PostErrorCode.INVALID_POST_STATE -> HttpStatus.CONFLICT
            PostErrorCode.UNAUTHORIZED_ACCESS -> HttpStatus.FORBIDDEN

            // Title Validation
            PostErrorCode.BLANK_TITLE,
            PostErrorCode.TITLE_TOO_LONG,
            PostErrorCode.TITLE_CONTAINS_INVALID_CHAR,

            // Content Validation
            PostErrorCode.BLANK_CONTENT,
            PostErrorCode.CONTENT_TOO_SHORT,
            PostErrorCode.CONTENT_TOO_LONG,

            // PostId Validation
            PostErrorCode.INVALID_POST_ID_FORMAT,
            -> HttpStatus.BAD_REQUEST

            // Author Validation
            PostErrorCode.INVALID_AUTHOR -> HttpStatus.BAD_REQUEST
            PostErrorCode.AUTHOR_NOT_FOUND -> HttpStatus.NOT_FOUND

            // Tag Validation
            PostErrorCode.BLANK_TAG,
            PostErrorCode.TAG_TOO_SHORT,
            PostErrorCode.TAG_TOO_LONG,
            PostErrorCode.TAG_CONTAINS_INVALID_CHAR,
            PostErrorCode.TOO_MANY_TAGS,
            PostErrorCode.DUPLICATE_TAG,
            -> HttpStatus.BAD_REQUEST

            // Application Level
            PostErrorCode.CREATE_POST_FAILED,
            PostErrorCode.UPDATE_POST_FAILED,
            PostErrorCode.DELETE_POST_FAILED,
            PostErrorCode.PUBLISH_POST_FAILED,
            -> HttpStatus.INTERNAL_SERVER_ERROR

            // Generic
            PostErrorCode.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST
            PostErrorCode.DOMAIN_ERROR,
            PostErrorCode.INTERNAL_ERROR,
            -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    fun mapToMessage(
        postErrorCode: PostErrorCode,
        params: Array<out Any?>,
    ): String {
        val baseMessage =
            when (postErrorCode) {
                // Post Status
                PostErrorCode.POST_NOT_FOUND ->
                    if (params.isNotEmpty()) {
                        "게시글(${params[0]})을 찾을 수 없습니다"
                    } else {
                        "게시글을 찾을 수 없습니다"
                    }
                PostErrorCode.POST_DELETED -> "이미 삭제된 게시글입니다"
                PostErrorCode.POST_DRAFT -> "아직 작성 중인 게시글입니다"
                PostErrorCode.POST_IN_REVIEW -> "검토 중인 게시글입니다"

                // Post Management
                PostErrorCode.DUPLICATE_TITLE ->
                    if (params.isNotEmpty()) {
                        "이미 사용 중인 제목(${params[0]})입니다"
                    } else {
                        "이미 사용 중인 제목입니다"
                    }
                PostErrorCode.INVALID_POST_STATE ->
                    if (params.isNotEmpty()) {
                        "유효하지 않은 게시글 상태(${params[0]})입니다"
                    } else {
                        "유효하지 않은 게시글 상태입니다"
                    }
                PostErrorCode.UNAUTHORIZED_ACCESS -> "게시글에 대한 접근 권한이 없습니다"

                // Title Validation
                PostErrorCode.BLANK_TITLE -> "제목은 필수 입력 항목입니다"
                PostErrorCode.TITLE_TOO_LONG ->
                    if (params.size > 1) {
                        "제목은 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "제목이 너무 깁니다"
                    }
                PostErrorCode.TITLE_CONTAINS_INVALID_CHAR ->
                    if (params.isNotEmpty()) {
                        "제목에 허용되지 않는 문자(${params[0]})가 포함되어 있습니다"
                    } else {
                        "제목에 허용되지 않는 문자가 포함되어 있습니다"
                    }

                // Content Validation
                PostErrorCode.BLANK_CONTENT -> "본문은 필수 입력 항목입니다"
                PostErrorCode.CONTENT_TOO_SHORT ->
                    if (params.size > 1) {
                        "본문은 최소 ${params[1]}자 이상이어야 합니다"
                    } else {
                        "본문이 너무 짧습니다"
                    }
                PostErrorCode.CONTENT_TOO_LONG ->
                    if (params.size > 1) {
                        "본문은 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "본문이 너무 깁니다"
                    }

                // PostId Validation
                PostErrorCode.INVALID_POST_ID_FORMAT ->
                    if (params.isNotEmpty()) {
                        "유효하지 않은 게시글 ID 형식입니다: ${params[0]}"
                    } else {
                        "유효하지 않은 게시글 ID 형식입니다"
                    }

                // Author Validation
                PostErrorCode.INVALID_AUTHOR ->
                    if (params.isNotEmpty()) {
                        "유효하지 않은 작성자(${params[0]})입니다"
                    } else {
                        "유효하지 않은 작성자입니다"
                    }
                PostErrorCode.AUTHOR_NOT_FOUND ->
                    if (params.isNotEmpty()) {
                        "작성자(${params[0]})를 찾을 수 없습니다"
                    } else {
                        "작성자를 찾을 수 없습니다"
                    }

                // Tag Validation
                PostErrorCode.BLANK_TAG -> "태그는 필수 입력 항목입니다"
                PostErrorCode.TAG_TOO_SHORT ->
                    if (params.size > 1) {
                        "태그는 최소 ${params[1]}자 이상이어야 합니다"
                    } else {
                        "태그가 너무 짧습니다"
                    }
                PostErrorCode.TAG_TOO_LONG ->
                    if (params.size > 1) {
                        "태그는 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "태그가 너무 깁니다"
                    }
                PostErrorCode.TAG_CONTAINS_INVALID_CHAR ->
                    if (params.isNotEmpty()) {
                        "태그에 허용되지 않는 문자가 포함되어 있습니다: ${params[0]}"
                    } else {
                        "태그에 허용되지 않는 문자가 포함되어 있습니다"
                    }
                PostErrorCode.TOO_MANY_TAGS ->
                    if (params.isNotEmpty()) {
                        "태그는 최대 ${params[0]}개까지 등록 가능합니다"
                    } else {
                        "태그 개수가 제한을 초과했습니다"
                    }
                PostErrorCode.DUPLICATE_TAG ->
                    if (params.isNotEmpty()) {
                        "중복된 태그입니다: ${params[0]}"
                    } else {
                        "중복된 태그입니다"
                    }

                // Application Level
                PostErrorCode.CREATE_POST_FAILED -> "게시글 생성 처리 중 오류가 발생했습니다"
                PostErrorCode.UPDATE_POST_FAILED -> "게시글 수정 처리 중 오류가 발생했습니다"
                PostErrorCode.DELETE_POST_FAILED -> "게시글 삭제 처리 중 오류가 발생했습니다"
                PostErrorCode.PUBLISH_POST_FAILED -> "게시글 발행 처리 중 오류가 발생했습니다"

                // Generic
                PostErrorCode.VALIDATION_ERROR ->
                    if (params.isNotEmpty()) {
                        "검증 실패: ${params[0]}"
                    } else {
                        "검증 실패"
                    }
                PostErrorCode.DOMAIN_ERROR -> "도메인 처리 중 오류가 발생했습니다"
                PostErrorCode.INTERNAL_ERROR -> "시스템 오류가 발생했습니다"
            }

        return baseMessage
    }
}
