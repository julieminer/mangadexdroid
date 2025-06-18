package com.melonhead.lib_app_data.models

sealed class RenderStyle {
    data object Native: RenderStyle()
    data object WebView: RenderStyle()
    data object Browser: RenderStyle()

    override fun toString(): String {
        return when (this) {
            is Native -> "native"
            is WebView -> "webview"
            is Browser -> "browser"
        }
    }

    companion object {
        fun fromString(style: String?): RenderStyle? {
            return when (style?.lowercase()) {
                "native" -> Native
                "webview" -> WebView
                "browser" -> Browser
                else -> null
            }
        }
    }
}
