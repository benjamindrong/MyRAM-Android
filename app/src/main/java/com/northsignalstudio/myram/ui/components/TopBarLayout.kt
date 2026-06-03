package com.northsignalstudio.myram.ui.components

data class TopBarLayoutResult(
    val visibleActionCount: Int,
    val showOverflowButton: Boolean,
    val ellipsizeTitle: Boolean
)

fun computeTopBarLayout(
    totalWidthPx: Float,
    leadingWidthPx: Float,
    titleWidthPx: Float,
    actionCount: Int,
    actionWidthPx: Float,
    spacingPx: Float,
    overflowButtonWidthPx: Float
): TopBarLayoutResult {
    if (actionCount <= 0) {
        val fits = leadingWidthPx + titleWidthPx <= totalWidthPx
        return TopBarLayoutResult(0, false, !fits)
    }

    for (visibleCount in actionCount downTo 0) {
        val hiddenCount = actionCount - visibleCount
        val showOverflow = hiddenCount > 0

        val visibleActionsWidth =
            if (visibleCount == 0) 0f
            else (visibleCount * actionWidthPx) + ((visibleCount - 1) * spacingPx)

        val trailingControlsWidth =
            visibleActionsWidth +
                if (showOverflow) {
                    overflowButtonWidthPx + if (visibleCount > 0) spacingPx else 0f
                } else {
                    0f
                }

        val neededWidth =
            leadingWidthPx +
                titleWidthPx +
                (if (trailingControlsWidth > 0f) spacingPx else 0f) +
                trailingControlsWidth

        if (neededWidth <= totalWidthPx) {
            return TopBarLayoutResult(
                visibleActionCount = visibleCount,
                showOverflowButton = showOverflow,
                ellipsizeTitle = false
            )
        }
    }

    val titleFitsWithoutTrailing = leadingWidthPx + titleWidthPx <= totalWidthPx
    return TopBarLayoutResult(
        visibleActionCount = 0,
        showOverflowButton = false,
        ellipsizeTitle = !titleFitsWithoutTrailing
    )
}
