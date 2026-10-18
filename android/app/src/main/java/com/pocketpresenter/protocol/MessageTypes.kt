package com.pocketpresenter.protocol

object MessageTypes {
    const val HELLO = "hello"
    const val HELLO_ACK = "hello.ack"
    const val SESSION_PING = "session.ping"
    const val SESSION_PONG = "session.pong"
    const val STATE_REQUEST = "state.request"
    const val STATE_UPDATE = "state.update"
    const val POINTER_DELTA = "pointer.delta"
    const val POINTER_BUTTON = "pointer.button"
    const val POINTER_SCROLL = "pointer.scroll"
    const val SLIDE_NEXT = "slide.next"
    const val SLIDE_PREVIOUS = "slide.previous"
    const val EFFECT_SET = "effect.set"
    const val DRAW_START = "draw.start"
    const val DRAW_POINT = "draw.point"
    const val DRAW_END = "draw.end"
    const val COMMAND_ACK = "command.ack"
    const val COMMAND_ERROR = "command.error"

    val ALL_VALID_TYPES = setOf(
        HELLO,
        HELLO_ACK,
        SESSION_PING,
        SESSION_PONG,
        STATE_REQUEST,
        STATE_UPDATE,
        POINTER_DELTA,
        POINTER_BUTTON,
        POINTER_SCROLL,
        SLIDE_NEXT,
        SLIDE_PREVIOUS,
        EFFECT_SET,
        DRAW_START,
        DRAW_POINT,
        DRAW_END,
        COMMAND_ACK,
        COMMAND_ERROR
    )

    fun isValidType(type: String?): Boolean = type != null && type in ALL_VALID_TYPES
}
