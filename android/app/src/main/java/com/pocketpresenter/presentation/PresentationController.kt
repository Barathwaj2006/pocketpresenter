package com.pocketpresenter.presentation

/**
 * Presentation slide navigation actions.
 * Maps hardware volume buttons and UI gestures to slide commands.
 */
interface PresentationController {
    fun nextSlide()
    fun previousSlide()
    fun goToSlide(slideIndex: Int)
}
