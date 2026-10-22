package com.example.bodycamoutdoorgear.giveaway

/**
 * Everything on the Giveaway card that varies per item. The AI step fills this
 * in later; until then the card uses [fallback] text the user can edit.
 */
data class GiveawayCardContent(
    val itemName: String,
    val facts: List<String>,
    val comment: String,
    /** Optional short safety note under the facts, for example "Please test it before using." */
    val note: String = "",
    /** When the item was shot, printed on the card with "Shot live with LBC". */
    val dateMillis: Long = System.currentTimeMillis(),
) {
    /** Trims and limits the user's edits so edited text still fits the card; blank facts are dropped. */
    fun cleaned(): GiveawayCardContent = copy(
        itemName = itemName.trim().take(MAX_NAME).ifEmpty { "Free item" },
        facts = facts.map { it.trim().take(MAX_FACT) }.filter { it.isNotEmpty() }.take(MAX_FACTS),
        comment = comment.trim().take(MAX_COMMENT),
        note = note.trim().take(MAX_NOTE),
    )

    companion object {
        const val MAX_NAME = 28
        const val MAX_FACT = 26
        const val MAX_FACTS = 3
        const val MAX_COMMENT = 90
        const val MAX_NOTE = 44

        val generalComments = listOf(
            "Old to me, new to you.",
            "Too good to throw away.",
            "It still has a lot to give.",
            "Time for a new home.",
            "One person's extra is another person's treasure.",
            "Free, and still full of stories.",
            "Good things deserve a second life.",
            "Someone out there needs this.",
            "Mystery item! Ask before you take it.",
        )

        /**
         * What the card says when nothing better is available. Deliberately
         * generic so it is never wrong about the item; the user can edit every word.
         */
        fun fallback(dateMillis: Long = System.currentTimeMillis()) = GiveawayCardContent(
            itemName = "Free item",
            facts = listOf("Ask me for details"),
            comment = generalComments.random(),
            dateMillis = dateMillis,
        )
    }
}

/** The fixed words printed on the card. English only for now. */
object GiveawayCardStrings {
    const val HEADER = "GIVEAWAY"
    const val FREE = "FREE"
    const val CONTACT = "Want it? Contact me!"
    const val SAYS = "LBC SAYS"
    const val MADE_BY = "Made by LBC"
    const val FOOTER_1 = "Get LBC (Low Battery Camera)"
    const val FOOTER_2 = "on App Store and Google Play"
    const val LEARN_MORE = "Learn more and safety tips:"
    /** The page the card points to. It must stay live: cards already shared carry this address. */
    const val WEBSITE = "gogoodmood.com/giveaway"
    const val SHOT_LIVE = "Shot live with LBC"
}
