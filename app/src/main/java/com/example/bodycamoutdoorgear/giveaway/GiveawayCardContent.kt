package com.example.bodycamoutdoorgear.giveaway

import com.example.bodycamoutdoorgear.i18n.Tr

/**
 * Everything on the Giveaway card that varies per item. The AI step fills this
 * in; without it the card uses [fallback] text the user can edit.
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
        itemName = itemName.trim().take(MAX_NAME).ifEmpty { GiveawayCardWords.current.fallbackName },
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

        /**
         * What the card says when nothing better is available. Deliberately
         * generic so it is never wrong about the item; the user can edit every
         * word. In the card's language.
         */
        fun fallback(dateMillis: Long = System.currentTimeMillis()): GiveawayCardContent {
            val words = GiveawayCardWords.current
            return GiveawayCardContent(
                itemName = words.fallbackName,
                facts = listOf(words.fallbackFact),
                comment = words.comments.random(),
                dateMillis = dateMillis,
            )
        }
    }
}

/**
 * The language the card is written in: the app's language (chosen in Settings,
 * or the phone's), the same codes the server uses.
 */
private val cardLanguage: String get() = Tr.code

/**
 * The fixed words printed on the card, one table per language. Another
 * language is one more table here (and on the server).
 */
data class GiveawayCardStrings(
    val header: String,
    val free: String,
    val contact: String,
    val says: String,
    val madeBy: String,
    val footer1: String,
    val footer2: String,
    val learnMore: String,
    val shotLive: String,
) {
    companion object {
        /** The page the card points to. It must stay live: cards already shared carry this address. */
        const val WEBSITE = "gogoodmood.com/giveaway"

        val current: GiveawayCardStrings get() = forLanguage(cardLanguage)

        fun forLanguage(code: String): GiveawayCardStrings = when (code) {
            "de" -> GERMAN
            "id" -> INDONESIAN
            "ja" -> JAPANESE
            "fr" -> FRENCH
            "ko" -> KOREAN
            "zh-Hans" -> CHINESE_SIMPLIFIED
            "zh-Hant" -> CHINESE_TRADITIONAL
            else -> ENGLISH
        }

        private val ENGLISH = GiveawayCardStrings(
            header = "GIVEAWAY", free = "FREE", contact = "Want it? Contact me!", says = "LBC SAYS",
            madeBy = "Made by LBC", footer1 = "Get LBC (Low Battery Camera)", footer2 = "on App Store and Google Play",
            learnMore = "Learn more and safety tips:", shotLive = "Shot live with LBC",
        )
        private val GERMAN = GiveawayCardStrings(
            header = "ZU VERSCHENKEN", free = "GRATIS", contact = "Interesse? Schreib mir!", says = "LBC SAGT",
            madeBy = "Gemacht mit LBC", footer1 = "Hol dir LBC (Low Battery Camera)", footer2 = "im App Store und bei Google Play",
            learnMore = "Mehr erfahren und Sicherheitstipps:", shotLive = "Live mit LBC aufgenommen",
        )
        private val INDONESIAN = GiveawayCardStrings(
            header = "BAGI-BAGI", free = "GRATIS", contact = "Mau? Hubungi aku!", says = "LBC BILANG",
            madeBy = "Dibuat dengan LBC", footer1 = "Dapatkan LBC (Low Battery Camera)", footer2 = "di App Store dan Google Play",
            learnMore = "Info lengkap dan tips keamanan:", shotLive = "Difoto langsung dengan LBC",
        )
        private val JAPANESE = GiveawayCardStrings(
            header = "お譲りします", free = "無料", contact = "ほしい方はご連絡ください！", says = "LBCのひとこと",
            madeBy = "LBCで作成", footer1 = "LBC（Low Battery Camera）を入手", footer2 = "App StoreとGoogle Playで",
            learnMore = "詳しくは・安全のヒント:", shotLive = "LBCでその場で撮影",
        )
        private val FRENCH = GiveawayCardStrings(
            header = "À DONNER", free = "GRATUIT", contact = "Intéressé ? Contactez-moi !", says = "LBC DIT",
            madeBy = "Fait avec LBC", footer1 = "Téléchargez LBC (Low Battery Camera)", footer2 = "sur l'App Store et Google Play",
            learnMore = "En savoir plus et conseils de sécurité :", shotLive = "Pris sur le vif avec LBC",
        )
        private val KOREAN = GiveawayCardStrings(
            header = "무료 나눔", free = "무료", contact = "원하세요? 연락 주세요!", says = "LBC 한마디",
            madeBy = "LBC로 제작", footer1 = "LBC(Low Battery Camera) 받기", footer2 = "App Store와 Google Play에서",
            learnMore = "자세한 정보와 안전 수칙:", shotLive = "LBC로 현장 촬영",
        )
        private val CHINESE_SIMPLIFIED = GiveawayCardStrings(
            header = "免费赠送", free = "免费", contact = "想要吗？联系我！", says = "LBC 说",
            madeBy = "LBC 制作", footer1 = "获取 LBC（Low Battery Camera）", footer2 = "App Store 和 Google Play 均可下载",
            learnMore = "了解更多和安全提示：", shotLive = "用 LBC 现场拍摄",
        )
        private val CHINESE_TRADITIONAL = GiveawayCardStrings(
            header = "免費贈送", free = "免費", contact = "想要嗎？聯絡我！", says = "LBC 說",
            madeBy = "LBC 製作", footer1 = "取得 LBC（Low Battery Camera）", footer2 = "App Store 與 Google Play 皆可下載",
            learnMore = "瞭解更多與安全提示：", shotLive = "用 LBC 現場拍攝",
        )
    }
}

/**
 * The words used when there is no AI text (offline, limit reached, AI not
 * available), in the card's language. The user can edit all of it.
 */
data class GiveawayCardWords(
    val fallbackName: String,
    val fallbackFact: String,
    val comments: List<String>,
) {
    companion object {
        val current: GiveawayCardWords get() = forLanguage(cardLanguage)

        fun forLanguage(code: String): GiveawayCardWords = when (code) {
            "de" -> GERMAN
            "id" -> INDONESIAN
            "ja" -> JAPANESE
            "fr" -> FRENCH
            "ko" -> KOREAN
            "zh-Hans" -> CHINESE_SIMPLIFIED
            "zh-Hant" -> CHINESE_TRADITIONAL
            else -> ENGLISH
        }

        private val ENGLISH = GiveawayCardWords(
            "Free item", "Ask me for details",
            listOf(
                "Old to me, new to you.", "Too good to throw away.", "It still has a lot to give.",
                "Time for a new home.", "One person's extra is another person's treasure.",
                "Free, and still full of stories.", "Good things deserve a second life.",
                "Someone out there needs this.", "Mystery item! Ask before you take it.",
            ),
        )
        private val GERMAN = GiveawayCardWords(
            "Zu verschenken", "Frag mich nach Details",
            listOf(
                "Für mich alt, für dich neu.", "Zu gut zum Wegwerfen.", "Es hat noch viel zu geben.",
                "Zeit für ein neues Zuhause.", "Was dem einen zu viel ist, ist dem anderen ein Schatz.",
                "Gratis und voller Geschichten.", "Gute Dinge verdienen ein zweites Leben.",
                "Irgendjemand braucht das hier.", "Überraschung! Frag nach, bevor du es mitnimmst.",
            ),
        )
        private val INDONESIAN = GiveawayCardWords(
            "Barang gratis", "Tanya aku untuk detail",
            listOf(
                "Lama bagiku, baru bagimu.", "Sayang kalau dibuang.", "Masih banyak manfaatnya.",
                "Saatnya punya rumah baru.", "Yang berlebih bagi satu orang adalah harta bagi orang lain.",
                "Gratis, dan penuh cerita.", "Barang bagus pantas dapat kehidupan kedua.",
                "Pasti ada yang membutuhkan ini.", "Barang misterius! Tanya dulu sebelum diambil.",
            ),
        )
        private val JAPANESE = GiveawayCardWords(
            "お譲り品", "詳細はお尋ねください",
            listOf(
                "私には古くても、あなたには新しい。", "捨てるにはもったいない。", "まだまだ活躍できます。",
                "新しい家を探しています。", "ある人の余りものは、別の人の宝物。",
                "無料で、思い出もいっぱい。", "いいものには第二の人生を。",
                "きっと必要な人がいます。", "ミステリーアイテム！受け取る前に聞いてね。",
            ),
        )
        private val FRENCH = GiveawayCardWords(
            "À donner", "Détails sur demande",
            listOf(
                "Vieux pour moi, neuf pour vous.", "Trop bien pour finir à la poubelle.", "Il a encore beaucoup à offrir.",
                "Il est temps de trouver un nouveau foyer.", "Ce qui est en trop pour l'un est un trésor pour l'autre.",
                "Gratuit, et plein d'histoires.", "Les bonnes choses méritent une seconde vie.",
                "Quelqu'un en a sûrement besoin.", "Objet mystère ! Demandez avant de le prendre.",
            ),
        )
        private val KOREAN = GiveawayCardWords(
            "무료 나눔", "자세히 문의해 주세요",
            listOf(
                "제겐 오래됐지만, 당신에겐 새것.", "버리기엔 너무 아까워요.", "아직 쓸모가 많아요.",
                "새 주인을 기다려요.", "누군가의 남는 것은 다른 사람의 보물.",
                "무료 나눔, 이야기는 가득.", "좋은 물건은 두 번째 삶을 누려야죠.",
                "분명 필요한 사람이 있을 거예요.", "미스터리 물건! 가져가기 전에 물어보세요.",
            ),
        )
        private val CHINESE_SIMPLIFIED = GiveawayCardWords(
            "免费赠送", "详情请咨询",
            listOf(
                "对我来说旧了，对你来说是新的。", "扔掉太可惜了。", "它还能发挥很多作用。",
                "该换个新家了。", "一个人的多余，是另一个人的宝贝。",
                "免费赠送，还有很多故事。", "好东西值得有第二次生命。",
                "一定有人需要它。", "神秘物品！拿走前请先问问。",
            ),
        )
        private val CHINESE_TRADITIONAL = GiveawayCardWords(
            "免費贈送", "詳情請洽詢",
            listOf(
                "對我來說舊了，對你來說是新的。", "丟掉太可惜了。", "它還能發揮很多作用。",
                "該換個新家了。", "一個人的多餘，是另一個人的寶貝。",
                "免費贈送，還有很多故事。", "好東西值得有第二次生命。",
                "一定有人需要它。", "神祕物品！拿走前請先問問。",
            ),
        )
    }
}
