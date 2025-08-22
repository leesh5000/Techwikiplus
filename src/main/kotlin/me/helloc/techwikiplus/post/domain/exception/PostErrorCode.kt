package me.helloc.techwikiplus.post.domain.exception

enum class PostErrorCode {
    // Post Status
    POST_NOT_FOUND,
    POST_DELETED,
    POST_DRAFT,
    POST_IN_REVIEW,

    // Post Management
    DUPLICATE_TITLE,
    INVALID_POST_STATE,
    FORBIDDEN_POST_ROLE,

    // Title Validation
    BLANK_TITLE,
    TITLE_TOO_LONG,
    TITLE_CONTAINS_INVALID_CHAR,

    // Content Validation
    BLANK_CONTENT,
    CONTENT_TOO_SHORT,
    CONTENT_TOO_LONG,

    // PostId Validation
    INVALID_POST_ID_FORMAT,

    // Author Validation
    INVALID_AUTHOR,
    AUTHOR_NOT_FOUND,

    // Application Level
    CREATE_POST_FAILED,
    UPDATE_POST_FAILED,
    DELETE_POST_FAILED,
    PUBLISH_POST_FAILED,

    // Tag Validation
    BLANK_TAG,
    TAG_TOO_SHORT,
    TAG_TOO_LONG,
    TAG_CONTAINS_INVALID_CHAR,
    TOO_MANY_TAGS,
    DUPLICATE_TAG,

    // Generic
    VALIDATION_ERROR,
    DOMAIN_ERROR,
    INTERNAL_ERROR,
}
