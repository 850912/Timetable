package com.hufeng943.timetable.shared.importexport

/** Shared protocol constants for transferring timetable files through the Wear Data Layer. */
object WearFileTransferProtocol {
    const val PATH_PREFIX = "/timetable/file-transfer/v1"

    fun path(requestId: String): String = "$PATH_PREFIX/$requestId"

    const val KEY_KIND = "kind"
    const val KEY_REQUEST_ID = "requestId"
    const val KEY_TARGET_NODE_ID = "targetNodeId"
    const val KEY_SOURCE_NODE_ID = "sourceNodeId"
    const val KEY_FILE_NAME = "fileName"
    const val KEY_MIME_TYPE = "mimeType"
    const val KEY_ASSET = "asset"
    const val KEY_ERROR_MESSAGE = "errorMessage"

    const val KIND_WEAR_EXPORT = "wear_export"
    const val KIND_PHONE_IMPORT_RESULT = "phone_import_result"
    const val KIND_PHONE_PUSH_TIMETABLES = "phone_push_timetables"
    const val KIND_SYNC_BATCH = "sync_batch"
    const val KIND_SYNC_ACK = "sync_ack"

    const val URI_SCHEME = "timetable"
    const val URI_HOST = "transfer"
    const val URI_MODE = "mode"
    const val URI_REQUEST_ID = "requestId"
    const val URI_SOURCE_NODE_ID = "sourceNodeId"

    const val MODE_IMPORT = "import"
}
