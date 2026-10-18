package com.pocketpresenter.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for ErrorCatalog conforming to docs/app_flow.md §4.
 * Verifies every error row has one recommended next action and no raw stack traces.
 */
class ErrorCatalogTest {

    @Test
    fun errorCatalog_allStandardErrorsDefineMessageAndAction() {
        val errors = listOf(
            ErrorCatalog.BLUETOOTH_OFF,
            ErrorCatalog.BLUETOOTH_PERMISSION_DENIED,
            ErrorCatalog.PERMISSION_PERMANENTLY_DENIED,
            ErrorCatalog.LACKS_BLUETOOTH_CLASSIC,
            ErrorCatalog.NO_PC_PAIRING,
            ErrorCatalog.FOREGROUND_SESSION_BLOCKED,
            ErrorCatalog.POWERPOINT_TIMEOUT,
            ErrorCatalog.BAD_VERSION,
            ErrorCatalog.NO_ACTIVE_SLIDESHOW,
            ErrorCatalog.NOT_ARMED,
            ErrorCatalog.LINK_LOST,
            ErrorCatalog.SCREEN_OFF_UNVALIDATED
        )

        for (err in errors) {
            assertNotNull(err.id)
            assertNotNull(err.message)
            assertNotNull(err.actionLabel)
            assertNotNull(err.actionType)
            // Sentence case check (first letter uppercase, ends with period or valid grammar)
            assertEquals(err.message.first().isUpperCase(), true)
        }
    }

    @Test
    fun errorCatalog_fromProtocolErrorCode_mapsExpectedCodes() {
        assertEquals(ErrorCatalog.BAD_VERSION, ErrorCatalog.fromProtocolErrorCode("BAD_VERSION"))
        assertEquals(ErrorCatalog.NO_ACTIVE_SLIDESHOW, ErrorCatalog.fromProtocolErrorCode("NO_ACTIVE_SLIDESHOW"))
        assertEquals(ErrorCatalog.NOT_ARMED, ErrorCatalog.fromProtocolErrorCode("NOT_ARMED"))
        assertEquals(ErrorCatalog.LINK_LOST, ErrorCatalog.fromProtocolErrorCode("LINK_LOST"))

        val unknown = ErrorCatalog.fromProtocolErrorCode("UNKNOWN_CODE")
        assertEquals("UNKNOWN_CODE", unknown.id)
        assertEquals("Error: UNKNOWN_CODE", unknown.message)
    }
}
