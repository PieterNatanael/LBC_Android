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
        fun fallback(mode: CardMode = CardMode.GIVEAWAY, dateMillis: Long = System.currentTimeMillis()): GiveawayCardContent {
            val words = GiveawayCardWords.current
            val name = when (mode) { CardMode.SALE -> words.saleFallbackName; CardMode.SWAP -> words.swapFallbackName; else -> words.fallbackName }
            val comments = when (mode) { CardMode.SALE -> words.saleComments; CardMode.SWAP -> words.swapComments; else -> words.comments }
            return GiveawayCardContent(
                itemName = name,
                facts = listOf(words.fallbackFact),
                comment = comments.random(),
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
    /** The sale card: its header, the contact line, and the price tag text when no price was entered. */
    val saleHeader: String,
    val saleContact: String,
    val priceDefault: String,
    /** The swap card: its header, the contact line, the tag's small label, and the tag text when nothing was typed. */
    val swapHeader: String,
    val swapContact: String,
    val wantedLabel: String,
    val wantedDefault: String,
) {
    companion object {
        /** The page the card points to. It must stay live: cards already shared carry this address. */
        const val WEBSITE = "gogoodmood.com/giveaway"

        val current: GiveawayCardStrings get() = forLanguage(cardLanguage)

        fun forLanguage(code: String): GiveawayCardStrings = when (code) {
            "de" -> GERMAN
            "id" -> INDONESIAN
            "fil" -> TAGLISH
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
            saleHeader = "FOR SALE", saleContact = "Interested? Contact me!", priceDefault = "Contact me for price",
            swapHeader = "SWAP", swapContact = "Want to swap? Contact me!",
            wantedLabel = "LOOKING FOR", wantedDefault = "Open to offers",
        )
        private val GERMAN = GiveawayCardStrings(
            header = "ZU VERSCHENKEN", free = "GRATIS", contact = "Interesse? Schreib mir!", says = "LBC SAGT",
            madeBy = "Gemacht mit LBC", footer1 = "Hol dir LBC (Low Battery Camera)", footer2 = "im App Store und bei Google Play",
            learnMore = "Mehr erfahren und Sicherheitstipps:", shotLive = "Live mit LBC aufgenommen",
            saleHeader = "ZU VERKAUFEN", saleContact = "Interesse? Schreib mir!", priceDefault = "Preis auf Anfrage",
            swapHeader = "ZUM TAUSCHEN", swapContact = "Lust zu tauschen? Schreib mir!",
            wantedLabel = "ICH SUCHE", wantedDefault = "Offen für Angebote",
        )
        private val INDONESIAN = GiveawayCardStrings(
            header = "BAGI-BAGI", free = "GRATIS", contact = "Mau? Hubungi aku!", says = "LBC BILANG",
            madeBy = "Dibuat dengan LBC", footer1 = "Dapatkan LBC (Low Battery Camera)", footer2 = "di App Store dan Google Play",
            learnMore = "Info lengkap dan tips keamanan:", shotLive = "Difoto langsung dengan LBC",
            saleHeader = "DIJUAL", saleContact = "Tertarik? Hubungi aku!", priceDefault = "Hubungi untuk harga",
            swapHeader = "TUKAR", swapContact = "Mau tukar? Hubungi aku!",
            wantedLabel = "MENCARI", wantedDefault = "Terbuka untuk tawaran",
        )
        private val TAGLISH = GiveawayCardStrings(
            header = "GIVEAWAY", free = "FREE", contact = "Gusto mo? I-message mo ako!", says = "SABI NG LBC",
            madeBy = "Gawa ng LBC", footer1 = "Kunin ang LBC (Low Battery Camera)", footer2 = "sa App Store at Google Play",
            learnMore = "Alamin pa at safety tips:", shotLive = "Kuha live gamit ang LBC",
            saleHeader = "FOR SALE", saleContact = "Interesado? I-message mo ako!", priceDefault = "I-message para sa presyo",
            swapHeader = "SWAP", swapContact = "Gusto mag-swap? I-message mo ako!",
            wantedLabel = "HINAHANAP", wantedDefault = "Bukas sa mga alok",
        )
        private val JAPANESE = GiveawayCardStrings(
            header = "お譲りします", free = "無料", contact = "ほしい方はご連絡ください！", says = "LBCのひとこと",
            madeBy = "LBCで作成", footer1 = "LBC（Low Battery Camera）を入手", footer2 = "App StoreとGoogle Playで",
            learnMore = "詳しくは・安全のヒント:", shotLive = "LBCでその場で撮影",
            saleHeader = "販売中", saleContact = "ご興味のある方は、ご連絡ください！", priceDefault = "価格はお問い合わせください",
            swapHeader = "交換", swapContact = "交換しませんか？ご連絡ください！",
            wantedLabel = "ほしいもの", wantedDefault = "ご提案ください",
        )
        private val FRENCH = GiveawayCardStrings(
            header = "À DONNER", free = "GRATUIT", contact = "Intéressé ? Contactez-moi !", says = "LBC DIT",
            madeBy = "Fait avec LBC", footer1 = "Téléchargez LBC (Low Battery Camera)", footer2 = "sur l'App Store et Google Play",
            learnMore = "En savoir plus et conseils de sécurité :", shotLive = "Pris sur le vif avec LBC",
            saleHeader = "À VENDRE", saleContact = "Intéressé ? Contactez-moi !", priceDefault = "Prix sur demande",
            swapHeader = "À ÉCHANGER", swapContact = "Un échange ? Contactez-moi !",
            wantedLabel = "JE CHERCHE", wantedDefault = "Ouvert aux propositions",
        )
        private val KOREAN = GiveawayCardStrings(
            header = "무료 나눔", free = "무료", contact = "원하세요? 연락 주세요!", says = "LBC 한마디",
            madeBy = "LBC로 제작", footer1 = "LBC(Low Battery Camera) 받기", footer2 = "App Store와 Google Play에서",
            learnMore = "자세한 정보와 안전 수칙:", shotLive = "LBC로 현장 촬영",
            saleHeader = "판매합니다", saleContact = "관심 있으세요? 연락 주세요!", priceDefault = "가격은 문의해 주세요",
            swapHeader = "교환", swapContact = "교환하실래요? 연락 주세요!",
            wantedLabel = "찾는 물건", wantedDefault = "제안 환영",
        )
        private val CHINESE_SIMPLIFIED = GiveawayCardStrings(
            header = "免费赠送", free = "免费", contact = "想要吗？联系我！", says = "LBC 说",
            madeBy = "LBC 制作", footer1 = "获取 LBC（Low Battery Camera）", footer2 = "App Store 和 Google Play 均可下载",
            learnMore = "了解更多和安全提示：", shotLive = "用 LBC 现场拍摄",
            saleHeader = "出售", saleContact = "感兴趣吗？联系我！", priceDefault = "价格请咨询",
            swapHeader = "交换", swapContact = "想交换？请联系我！",
            wantedLabel = "想换", wantedDefault = "欢迎提议",
        )
        private val CHINESE_TRADITIONAL = GiveawayCardStrings(
            header = "免費贈送", free = "免費", contact = "想要嗎？聯絡我！", says = "LBC 說",
            madeBy = "LBC 製作", footer1 = "取得 LBC（Low Battery Camera）", footer2 = "App Store 與 Google Play 皆可下載",
            learnMore = "瞭解更多與安全提示：", shotLive = "用 LBC 現場拍攝",
            saleHeader = "出售", saleContact = "有興趣嗎？聯絡我！", priceDefault = "價格請洽詢",
            swapHeader = "交換", swapContact = "想交換？請聯絡我！",
            wantedLabel = "想換", wantedDefault = "歡迎提議",
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
    /** The sale card's version of the above: no "free", no claims about price or condition. */
    val saleFallbackName: String,
    val saleComments: List<String>,
    /** The swap card's version: no money, no claims about value. */
    val swapFallbackName: String,
    val swapComments: List<String>,
) {
    companion object {
        val current: GiveawayCardWords get() = forLanguage(cardLanguage)

        fun forLanguage(code: String): GiveawayCardWords = when (code) {
            "de" -> GERMAN
            "id" -> INDONESIAN
            "fil" -> TAGLISH
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
            "Item for sale",
            listOf(
                "Looking for a new owner.",
                "Ready for its next adventure.",
                "Time to find a new home.",
                "Someone out there is looking for this.",
                "Ask me anything about it.",
                "Good things find good homes.",
                "A new owner is all it needs.",
                "Ready to move on.",
                "Message me to see it in person.",
            ),
            "Item to swap",
            listOf(
                "Looking for a good swap.",
                "Ready for a new owner.",
                "Swap it and give it a second life.",
                "Tell me what you have to swap.",
                "Let's find a swap that works for both of us.",
                "One person's item, another person's find.",
                "Ask me about swapping.",
                "Message me with your idea.",
                "Good things deserve a second life.",
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
            "Zu verkaufen",
            listOf(
                "Sucht einen neuen Besitzer.",
                "Bereit für sein nächstes Abenteuer.",
                "Zeit für ein neues Zuhause.",
                "Irgendwo sucht jemand genau das.",
                "Frag mich gern dazu.",
                "Gute Dinge finden gute Besitzer.",
                "Fehlt nur noch ein neuer Besitzer.",
                "Bereit für etwas Neues.",
                "Schreib mir, dann kannst du ihn dir ansehen.",
            ),
            "Zum Tauschen",
            listOf(
                "Sucht einen guten Tausch.",
                "Bereit für einen neuen Besitzer.",
                "Tauschen und ein zweites Leben schenken.",
                "Sag mir, was du zum Tauschen hast.",
                "Finden wir einen Tausch, der für beide passt.",
                "Des einen Ding, des anderen Fund.",
                "Frag mich gern nach einem Tausch.",
                "Schreib mir deine Idee.",
                "Gute Dinge verdienen ein zweites Leben.",
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
            "Barang dijual",
            listOf(
                "Mencari pemilik baru.",
                "Siap untuk petualangan berikutnya.",
                "Saatnya menemukan rumah baru.",
                "Pasti ada yang sedang mencari ini.",
                "Tanya apa saja tentang barang ini.",
                "Barang bagus menemukan pemilik yang tepat.",
                "Tinggal menunggu pemilik baru.",
                "Siap berpindah tangan.",
                "Hubungi aku untuk melihatnya langsung.",
            ),
            "Barang untuk ditukar",
            listOf(
                "Mencari tukar yang pas.",
                "Siap untuk pemilik baru.",
                "Tukar dan beri kehidupan kedua.",
                "Beri tahu aku apa yang kamu punya untuk ditukar.",
                "Ayo cari pertukaran yang cocok untuk kita berdua.",
                "Barang bagi satu orang, temuan bagi orang lain.",
                "Tanya aku soal tukar-menukar.",
                "Kirim idemu lewat pesan.",
                "Barang bagus pantas dapat kehidupan kedua.",
            ),
        )
        private val TAGLISH = GiveawayCardWords(
            "Libreng gamit", "Tanungin mo ako para sa detalye",
            listOf(
                "Luma sa akin, bago para sa'yo.", "Sayang naman kung itatapon.", "Marami pa itong magagawa.",
                "Oras na para maghanap ng bagong bahay.", "Ang sobra sa isa, kayamanan ng iba.",
                "Libre, at puno pa ng kuwento.", "Ang magagandang bagay, deserve ng second life.",
                "May nangangailangan nito, sigurado.", "Mystery item! Magtanong muna bago kunin.",
            ),
            "Binebenta",
            listOf(
                "Naghahanap ng bagong owner.",
                "Handa na sa susunod na adventure.",
                "Oras na para maghanap ng bagong tahanan.",
                "May naghahanap nito, sigurado.",
                "Magtanong ka lang tungkol dito.",
                "Ang magagandang bagay, napupunta sa tamang owner.",
                "Bagong owner na lang ang kulang.",
                "Handa nang lumipat ng kamay.",
                "I-message mo ako para makita mo ito nang personal.",
            ),
            "Gamit na i-swap",
            listOf(
                "Naghahanap ng magandang swap.",
                "Handa na para sa bagong owner.",
                "I-swap mo at bigyan ng second life.",
                "Sabihin mo kung ano ang puwede mong i-swap.",
                "Humanap tayo ng swap na okay para sa ating dalawa.",
                "Ang gamit ng isa, kayamanan ng iba.",
                "Magtanong ka tungkol sa swap.",
                "I-message mo ako ng idea mo.",
                "Ang magagandang bagay, deserve ng second life.",
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
            "販売品",
            listOf(
                "新しい持ち主を探しています。",
                "次の冒険の準備はできています。",
                "新しい家を探す時が来ました。",
                "きっと探している人がいます。",
                "気になることは、何でも聞いてください。",
                "いいものは、いい持ち主に出会います。",
                "あとは新しい持ち主を待つだけです。",
                "次の持ち主へ旅立つ準備ができました。",
                "ご連絡いただければ、実物をご覧いただけます。",
            ),
            "交換品",
            listOf(
                "いい交換相手を探しています。",
                "新しい持ち主を待っています。",
                "交換して、もうひとつの人生を。",
                "交換できるものを教えてください。",
                "お互いに納得できる交換をしましょう。",
                "ある人の持ち物は、別の人の掘り出し物。",
                "交換のご相談はお気軽に。",
                "アイデアをメッセージでお寄せください。",
                "いいものには、第二の人生を。",
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
            "À vendre",
            listOf(
                "Cherche un nouveau propriétaire.",
                "Prêt pour sa prochaine aventure.",
                "Il est temps de trouver un nouveau foyer.",
                "Quelqu'un cherche sûrement ceci.",
                "Posez-moi toutes vos questions.",
                "Les bonnes choses trouvent de bonnes mains.",
                "Il ne lui manque qu'un nouveau propriétaire.",
                "Prêt à changer de main.",
                "Écrivez-moi pour le voir en personne.",
            ),
            "Objet à échanger",
            listOf(
                "À la recherche d'un bon échange.",
                "Prêt pour un nouveau propriétaire.",
                "Échangez-le et offrez-lui une seconde vie.",
                "Dites-moi ce que vous avez à échanger.",
                "Trouvons un échange qui convienne à tous les deux.",
                "L'objet de l'un, la trouvaille de l'autre.",
                "Demandez-moi un échange.",
                "Écrivez-moi avec votre idée.",
                "Les belles choses méritent une seconde vie.",
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
            "판매 물품",
            listOf(
                "새 주인을 찾고 있어요.",
                "다음 모험을 떠날 준비가 되었어요.",
                "새 보금자리를 찾을 때예요.",
                "분명 찾는 분이 있을 거예요.",
                "궁금한 점은 무엇이든 물어보세요.",
                "좋은 물건은 좋은 주인을 만나요.",
                "새 주인만 있으면 돼요.",
                "새 주인에게 갈 준비가 됐어요.",
                "연락 주시면 직접 보실 수 있어요.",
            ),
            "교환 물품",
            listOf(
                "좋은 교환 상대를 찾고 있어요.",
                "새 주인을 기다리고 있어요.",
                "교환해서 두 번째 삶을 주세요.",
                "교환할 물건이 있다면 알려 주세요.",
                "서로에게 맞는 교환을 찾아봐요.",
                "누군가의 물건이 다른 사람에겐 보물이 될 수 있어요.",
                "교환에 대해 편하게 물어보세요.",
                "아이디어를 메시지로 보내 주세요.",
                "좋은 물건은 두 번째 삶을 누릴 자격이 있어요.",
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
            "出售物品",
            listOf(
                "正在寻找新主人。",
                "已准备好迎接下一次旅程。",
                "该找个新家了。",
                "一定有人正在找它。",
                "有什么想了解的，尽管问我。",
                "好东西会遇到好主人。",
                "只差一位新主人了。",
                "已准备好易主。",
                "联系我，可当面查看。",
            ),
            "交换物品",
            listOf(
                "想找个合适的交换。",
                "等待新主人。",
                "换一换，给它第二次生命。",
                "告诉我你有什么可以交换。",
                "一起找个对双方都合适的交换吧。",
                "这个人的旧物，是那个人的宝贝。",
                "想交换，欢迎来问我。",
                "把你的想法发消息给我吧。",
                "好东西值得拥有第二次生命。",
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
            "出售物品",
            listOf(
                "正在尋找新主人。",
                "已準備好迎接下一段旅程。",
                "該找個新家了。",
                "一定有人正在找它。",
                "有什麼想了解的，儘管問我。",
                "好東西會遇到好主人。",
                "只差一位新主人了。",
                "已準備好易主。",
                "聯絡我，可當面查看。",
            ),
            "交換物品",
            listOf(
                "想找個合適的交換。",
                "等待新主人。",
                "換一換，給它第二次生命。",
                "告訴我你有什麼可以交換。",
                "一起找個對雙方都合適的交換吧。",
                "這個人的舊物，是那個人的寶貝。",
                "想交換，歡迎來問我。",
                "把你的想法傳訊息給我吧。",
                "好東西值得擁有第二次生命。",
            ),
        )
    }
}
