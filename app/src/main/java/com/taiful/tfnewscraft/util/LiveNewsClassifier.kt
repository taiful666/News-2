package com.taiful.tfnewscraft.util

import com.taiful.tfnewscraft.data.model.CuratedLiveArticle
import com.taiful.tfnewscraft.data.model.LiveNewsTab
import com.taiful.tfnewscraft.data.model.NewsArticle
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Editorial filter & priority ranker for the Live News screen:
 * - Hard-blocks all Sports news (`খেলাধুলা`).
 * - Blocks ordinary Entertainment (`বিনোদন`), allowing ONLY viral/controversial entertainment stories.
 * - Blocks mundane/routine filler (`সাধারণ খবর`, lifestyle, horoscopes, recipes, jobs, tech specs, routine weather/meetings).
 * - Tab 1 (`বাংলাদেশ ও ভাইরাল`): prioritizes Bangladesh politics, viral topics, viral entertainment, and catchy nationwide events.
 * - Tab 2 (`আন্তর্জাতিক`): strongly prioritizes Iran / Israel / Palestine / Middle East hot news + major catchy global news.
 */
object LiveNewsClassifier {

    // --- 1. Hard Sports Exclusion ---
    private val SPORTS_URL_PATTERNS = listOf(
        "/sports", "/sport/", "/cricket", "/football", "/khela",
        "/ipl", "/bpl", "/fifa", "/tennis", "/badminton", "/hockey",
        "/olympic", "/world-cup", "/worldcup", "/athletics"
    )

    private val SPORTS_STRONG_KEYWORDS = listOf(
        "ক্রিকেট", "ফুটবল", "বিপিএল", "আইপিএল", "বিসিবি", "বাফুফে", "ফিফা", "উয়েফা",
        "বিশ্বকাপ", "এশিয়া কাপ", "চ্যাম্পিয়ন্স ট্রফি", "টেস্ট ম্যাচ", "ওয়ানডে", "টি-টোয়েন্টি",
        "টি২০", "টি-২০", "সুপার ওভার", "ইনিংস", "সেঞ্চুরি", "হাফ সেঞ্চুরি", "উইকেট",
        "বোলিং", "ব্যাটিং", "অলরাউন্ডার", "রান আউট", "এলবিডব্লিউ", "স্টাম্প", "পিচ",
        "টুর্নামেন্ট", "গোলকিপার", "পেনাল্টি", "ফ্রি কিক", "রেফারি", "অফসাইড",
        "কোয়ার্টার ফাইনাল", "সেমিফাইনাল", "প্রিমিয়ার লিগ", "লা লিগা", "বুন্দেসলিগা",
        "সিরি আ", "চ্যাম্পিয়ন্স লিগ", "কোপা আমেরিকা", "ইউরো কাপ", "অলিম্পিক",
        "ব্যাডমিন্টন", "টেনিস", "হকি", "কাবাডি", "বক্সিং", "রেসলিং",
        "মেসি", "রোনালদো", "নেইমার", "এমবাপ্পে", "হালান্ড", "ভিনিসিয়াস",
        "কোহলি", "রোহিত শর্মা", "ধোনি", "বাবর আজম", "তামিম ইকবাল", "মুশফিকুর রহিম",
        "মাহমুদউল্লাহ", "লিটন দাস", "তাসকিন", "মোস্তাফিজ", "মেহেদী হাসান মিরাজ",
        "নাজমুল হোসেন শান্ত", "হাথুরুসিংহে", "ফিল সিমন্স", "রিয়াল মাদ্রিদ", "বার্সেলোনা",
        "ম্যানচেস্টার", "লিভারপুল", "আর্সেনাল", "চেলসি", "বায়ার্ন", "পিএসজি",
        "ইন্টার মায়ামি", "আল নাসর", "পয়েন্ট টেবিল", "শিরোপা জয়", "সিরিজ জয়",
        "হোয়াইটওয়াশ", "হ্যাটট্রিক", "ক্রীড়া", "খেলোয়াড়", "অধিনায়ক", "স্কোয়াড", "একাদশ",
        "ক্রিকেটার", "ফুটবলার", "স্টেডিয়াম", "গ্যালারি", "মাঠে নামছে", "মুখোমুখি হচ্ছে",
        "খেলাধুলা", "খেলার খবর", "জয়ে ফিরল", "দুর্দান্ত জয়", "হারিয়ে ফাইনালে",
        "cricket", "football", "soccer", "ipl", "bpl", "fifa", "uefa", "world cup",
        "t20", "odi", "wicket", "batting", "bowling", "goalkeeper", "premier league",
        "la liga", "champions league", "messi", "ronaldo", "neymar", "mbappe", "kohli", "bcb"
    )

    // --- 2. Entertainment Detection + Viral Exception ---
    private val ENTERTAINMENT_URL_PATTERNS = listOf(
        "/entertainment", "/binodon", "/showbiz", "/glitz", "/cinema",
        "/bollywood", "/hollywood", "/dhallywood", "/tollywood", "/music",
        "/drama", "/natok", "/celebrity", "/lifestyle/entertainment", "/arts-and-entertainment"
    )

    private val ENTERTAINMENT_KEYWORDS = listOf(
        "বিনোদন", "সিনেমা", "চলচ্চিত্র", "নাটক", "ওয়েব সিরিজ", "ওটিটি", "গান", "মিউজিক ভিডিও",
        "অ্যালবাম", "কনসার্ট", "শুটিং", "ট্রেলার", "টিজার", "বক্স অফিস", "প্রেক্ষাগৃহ", "সিনেমা হল",
        "নায়ক", "নায়িকা", "অভিনেতা", "অভিনেত্রী", "কণ্ঠশিল্পী", "গায়ক", "গায়িকা", "মডেল",
        "নির্মাতা", "পরিচালক", "প্রযোজক", "বলিউড", "হলিউড", "ঢালিউড", "টালিউড",
        "শাকিব খান", "অপু বিশ্বাস", "বুবলী", "পরীমনি", "চঞ্চল চৌধুরী", "আফরান নিশো",
        "মেহজাবীন", "তানজিন তিশা", "সাবিলা নূর", "পূজা চেরী", "নুসরাত ফারিয়া", "জয়া আহসান",
        "তাহসান", "মিথিলা", "হিরো আলম", "জায়েদ খান", "সালমান খান", "শাহরুখ খান", "আমির খান",
        "দীপিকা", "আলিয়া", "ক্যাটরিনা", "রণবীর", "প্রিয়াঙ্কা", "বিগ বস", "অস্কার",
        "কান উৎসব", "মেরিল প্রথম আলো", "ফটোশুট", "তারকা", "অভিনয়", "মুভি", "ব্যান্ডদল",
        "bollywood", "hollywood", "dhallywood", "tollywood", "cinema", "movie", "actor",
        "actress", "singer", "web series", "trailer", "box office", "showbiz"
    )

    private val VIRAL_ENTERTAINMENT_SIGNALS = listOf(
        "ভাইরাল", "তোলপাড়", "বিতর্ক", "সমালোচনা", "ক্ষোভ", "ঝড়", "নেটদুনিয়ায়",
        "সোশ্যাল মিডিয়ায়", "ফেসবুকে ঝড়", "বিস্ফোরক", "ফাঁস", "ফোনালাপ", "অডিও ফাঁস",
        "ভিডিও ফাঁস", "গোপন", "স্ক্যান্ডাল", "কেলেঙ্কারি", "বিচ্ছেদ", "ডিভোর্স", "তালাক",
        "গোপন বিয়ে", "বিয়ে ভাঙল", "মামলা", "গ্রেপ্তার", "আটক", "রিমান্ড", "জেল",
        "কারাগার", "আদালত", "পরোয়ানা", "হুমকি", "হামলা", "মারধর", "আত্মহত্যা",
        "মৃত্যু", "মারা গেছেন", "রহস্যজনক", "প্রতারণা", "মানহানি", "আইনি নোটিশ",
        "বয়কট", "বয়কট", "নিষিদ্ধ", "সেন্সর", "কাঁদলেন", "ক্ষমা চাইলেন", "পাল্টাপাল্টি",
        "বাগ্‌বিতণ্ডা", "হেনস্তা", "ধর্ষণ", "শ্লীলতাহানি", "মাদক", "চাঞ্চল্য", "শোকের ছায়া",
        "viral", "scandal", "controversy", "arrest", "divorce", "leaked", "boycott", "lawsuit", "death"
    )

    // --- 3. Ordinary / Routine / Mundane Exclusion ---
    private val ROUTINE_URL_PATTERNS = listOf(
        "/lifestyle", "/life-style", "/recipe", "/food", "/fashion", "/beauty",
        "/health", "/fitness", "/horoscope", "/rashifol", "/astrology",
        "/tech", "/technology", "/gadget", "/science", "/education", "/campus",
        "/chakri", "/job", "/career", "/agriculture", "/krishi", "/travel", "/tourism",
        "/literature", "/sahitya", "/opinion", "/editorial", "/muktomot", "/feature",
        "/kids", "/shishu", "/weather", "/abohawa", "/corporate", "/press-release",
        "/classified", "/religion", "/islam", "/dhormo", "/living", "/youth", "/culture"
    )

    private val ROUTINE_HEADLINE_PATTERNS = listOf(
        "রাশিফল", "আজকের রাশিফল", "রেসিপি", "রান্না", "ত্বকের যত্ন", "চুলের যত্ন",
        "ওজন কমানো", "স্বাস্থ্য টিপস", "ভ্রমণ", "ঘুরতে যান", "নিয়োগ বিজ্ঞপ্তি", "চাকরির খবর",
        "আবেদন করুন", "বৃত্তি", "স্কলারশিপ", "ভর্তি পরীক্ষা", "ফলাফল প্রকাশ", "সেমিনার",
        "কর্মশালা", "আলোচনা সভা", "মতবিনিময় সভা", "সংবর্ধনা", "বৃক্ষরোপণ", "বইমেলা",
        "নতুন স্মার্টফোন", "দাম কত", "লঞ্চ হলো", "জেনে নিন", "কেন খাবেন", "উপকারিতা",
        "অপকারিতা", "আবহাওয়ার পূর্বাভাস", "কুয়াশা", "আজকের সোনার দাম", "আজকের টাকার রেট",
        "ইফতারের সময়", "সেহরির সময়", "নামাজের সময়সূচি", "কবিতা", "ছোটগল্প", "উপন্যাস",
        "প্রবন্ধ", "ফিচার", "horoscope", "recipe", "weather forecast", "job circular"
    )

    // --- 4. Middle East & Iran-Israel-Palestine Hotspots (TOP PRIORITY in International Tab) ---
    private val IRAN_KEYWORDS = listOf("ইরান", "তেহরান", "খামেনি", "পেজেশকিয়ান", "আইআরজিসি", "বিপ্লবী গার্ড", "হরমুজ", "iran", "tehran", "khamenei", "irgc", "hormuz")
    private val ISRAEL_KEYWORDS = listOf("ইসরায়েল", "ইসরাইল", "তেল আবিব", "নেতানিয়াহু", "মোসাদ", "আয়রন ডোম", "জেরুজালেম", "israel", "tel aviv", "netanyahu", "mossad", "idf", "jerusalem")
    private val PALESTINE_KEYWORDS = listOf("ফিলিস্তিন", "গাজা", "হামাস", "রাফাহ", "পশ্চিম তীর", "আল-আকসা", "palestine", "gaza", "hamas", "rafah", "west bank")
    private val MIDDLE_EAST_OTHER_KEYWORDS = listOf(
        "হিজবুল্লাহ", "লেবানন", "বৈরুত", "ইয়েমেন", "হুতি", "সিরিয়া", "দামেস্ক",
        "লোহিত সাগর", "মধ্যপ্রাচ্য", "ইরাক", "বাগদাদ", "জর্ডান",
        "hezbollah", "lebanon", "yemen", "houthi", "syria", "middle east", "red sea"
    )

    // --- 5. International Entities & Catchy World Events ---
    private val INTL_URL_PATTERNS = listOf(
        "/world", "/international", "/antoryatik", "/antorjatik", "/foreign",
        "/middle-east", "/global", "/asia", "/europe", "/america", "/international-news"
    )

    private val INTL_COUNTRY_LEADER_KEYWORDS = listOf(
        "যুক্তরাষ্ট্র", "আমেরিকা", "মার্কিন", "ট্রাম্প", "বাইডেন", "পেন্টাগন", "হোয়াইট হাউস", "সিআইএ",
        "রাশিয়া", "মস্কো", "পুতিন", "ইউক্রেন", "কিয়েভ", "জেলেনস্কি",
        "চীন", "বেইজিং", "শি জিনপিং", "তাইওয়ান", "উত্তর কোরিয়া", "কিম জং", "দক্ষিণ কোরিয়া", "জাপান", "টোকিও",
        "ভারত", "নয়াদিল্লি", "দিল্লি", "নরেন্দ্র মোদি", "মোদি", "পশ্চিমবঙ্গ", "কলকাতা", "মমতা", "কাশ্মীর", "আসাম", "ত্রিপুরা", "বিএসএফ",
        "পাকিস্তান", "ইসলামাবাদ", "ইমরান খান", "শাহবাজ", "আফগানিস্তান", "তালেবান", "কাবুল",
        "তুরস্ক", "এরদোয়ান", "আঙ্কারা", "সৌদি আরব", "রিয়াদ", "মক্কা", "মদিনা",
        "সংযুক্ত আরব আমিরাত", "দুবাই", "আবুধাবি", "কাতার", "দোহা", "কুয়েত", "ওমান", "মিশর", "কায়রো",
        "যুক্তরাজ্য", "ব্রিটেন", "লন্ডন", "ফ্রান্স", "প্যারিস", "জার্মানি", "বার্লিন", "ইউরোপ", "ইতালি", "কানাডা", "অস্ট্রেলিয়া",
        "মিয়ানমার", "আরাকান", "জান্তা", "রোহিঙ্গা", "নেপাল", "শ্রীলঙ্কা", "মালদ্বীপ", "মালয়েশিয়া", "সিঙ্গাপুর", "থাইল্যান্ড", "ইন্দোনেশিয়া",
        "সুদান", "ভেনেজুয়েলা", "ব্রাজিল", "মেক্সিকো", "ন্যাটো", "জাতিসংঘ", "নিরাপত্তা পরিষদ", "আইসিসি", "ব্রিকস",
        "usa", "america", "trump", "biden", "pentagon", "russia", "putin", "ukraine", "zelensky",
        "china", "taiwan", "india", "modi", "pakistan", "turkey", "erdogan", "saudi", "dubai", "qatar",
        "myanmar", "nato", "un ", "united nations", "north korea", "kim jong"
    )

    private val INTL_CATCHY_EVENT_KEYWORDS = listOf(
        "যুদ্ধ", "হামলা", "বিমান হামলা", "ড্রোন", "ক্ষেপণাস্ত্র", "মিসাইল", "রকেট", "বোমা", "বিস্ফোরণ",
        "পারমাণবিক", "পরমাণু", "নিষেধাজ্ঞা", "শুল্ক", "বাণিজ্য যুদ্ধ", "হুঁশিয়ারি", "হুমকি",
        "পাল্টা জবাব", "প্রতিশোধ", "সেনা", "সামরিক", "অভ্যুত্থান", "বিক্ষোভ", "সংঘর্ষ",
        "নিহত", "আহত", "ধ্বংস", "সংকট", "জরুরি অবস্থা", "যুদ্ধবিরতি", "চুক্তি", "সমঝোতা",
        "কূটনীতি", "রাষ্ট্রদূত", "সীমান্ত", "অনুপ্রবেশ", "গুপ্তচর", "সাইবার হামলা",
        "নির্বাসন", "গ্রেপ্তার", "পদত্যাগ", "নির্বাচন", "ক্ষমতাচ্যুত", "ভূমিকম্প", "সুনামি",
        "ঘূর্ণিঝড়", "দাবানল", "বিমান বিধ্বস্ত", "দুর্ঘটনা", "তেল", "জ্বালানি", "স্বর্ণ",
        "বিস্ফোরক", "চাঞ্চল্য", "তোলপাড়", "ভাইরাল", "অবাক", "ভয়াবহ", "চরম উত্তেজনা",
        "war", "strike", "missile", "drone", "nuclear", "sanction", "tariff", "attack",
        "warning", "threat", "coup", "crash", "earthquake", "crisis", "ceasefire", "explosion"
    )

    // --- 6. Bangladesh Politics, Viral Topics & Catchy Nationwide Events ---
    private val BD_POLITICS_URL_PATTERNS = listOf(
        "/politics", "/rajniti", "/election", "/nirbachon", "/parliament", "/national-politics"
    )

    private val BD_POLITICS_KEYWORDS = listOf(
        "রাজনীতি", "রাজনৈতিক", "নির্বাচন", "ভোট", "নির্বাচন কমিশন", "ইসি", "সংসদ", "সংসদ সদস্য", "এমপি",
        "মন্ত্রী", "মন্ত্রণালয়", "প্রধান উপদেষ্টা", "উপদেষ্টা", "ড. ইউনূস", "মুহাম্মদ ইউনূস", "ইউনূস",
        "তারেক রহমান", "খালেদা জিয়া", "শেখ হাসিনা", "হাসিনা", "মির্জা ফখরুল", "ফখরুল",
        "বিএনপি", "জামায়াত", "জামায়াতে ইসলামী", "শফিকুর রহমান", "এনসিপি", "জাতীয় নাগরিক পার্টি",
        "নাহিদ ইসলাম", "সারজিস", "হাসনাত", "আসিফ মাহমুদ", "মাহফুজ আলম",
        "আওয়ামী লীগ", "ছাত্রলীগ", "যুবলীগ", "ছাত্রদল", "শিবির", "ছাত্রশিবির", "হেফাজত",
        "ইসলামী আন্দোলন", "চরমোনাই", "গণঅধিকার", "নুরুল হক নুর", "জাতীয় পার্টি", "জিএম কাদের",
        "বৈষম্যবিরোধী", "সমন্বয়ক", "জুলাই", "গণঅভ্যুত্থান", "সংস্কার", "সংবিধান", "তত্ত্বাবধায়ক",
        "অন্তর্বর্তী সরকার", "সরকার", "সচিবালয়", "সেনাবাহিনী", "সেনাপ্রধান", "ওয়াকার",
        "পদত্যাগ", "অপসারণ", "নিষিদ্ধ", "সমাবেশ", "মিছিল", "হরতাল", "অবরোধ", "লংমার্চ",
        "ঘেরাও", "বিক্ষোভ", "আল্টিমেটাম", "হুঁশিয়ারি", "পাল্টা বক্তব্য", "বিবৃতি", "জোট",
        "আসন", "মনোনয়ন", "প্রজ্ঞাপন", "অধ্যাদেশ", "গেজেট", "ট্রাইব্যুনাল", "আন্তর্জাতিক অপরাধ ট্রাইব্যুনাল",
        "রাজপথে", "শাপলা", "আয়নাঘর", "বৈঠক", "দলীয়", "নেতাকর্মী", "বিভাগীয় সমাবেশ",
        "politics", "election", "bnp", "jamaat", "awami", "yunus", "tareque", "hasina"
    )

    private val VIRAL_CATCHY_KEYWORDS = listOf(
        "ভাইরাল", "নেটদুনিয়ায়", "সোশ্যাল মিডিয়ায়", "ফেসবুকে", "টিকটক", "ইউটিউব",
        "তোলপাড়", "চাঞ্চল্য", "ঝড়", "বিতর্ক", "বিস্ফোরক", "ফাঁস", "অডিও ফাঁস", "ভিডিও ফাঁস",
        "ফোনালাপ", "স্ক্যান্ডাল", "কেলেঙ্কারি", "রহস্য", "চমক", "অবাক", "অবিশ্বাস্য", "হতবাক",
        "ক্ষোভ", "সমালোচনা", "বয়কট", "বয়কট", "ট্রল", "লাইভে", "কান্না", "হুমকি", "পাল্টাপাল্টি",
        "হাতাহাতি", "মারামারি", "সংঘর্ষ", "তুলকালাম", "রণক্ষেত্র", "ককটেল", "গুলি", "লাঠিচার্জ",
        "টিয়ারশেল", "ভয়াবহ", "লোমহর্ষক", "অলৌকিক", "নজিরবিহীন", "viral", "leaked", "scandal", "controversy"
    )

    private val BD_NATIONAL_CATCHY_KEYWORDS = listOf(
        "দুর্নীতি", "দুদক", "অর্থপাচার", "পাচার", "ঘুষ", "চাঁদাবাজি", "সিন্ডিকেট", "দ্রব্যমূল্য",
        "বাজারদর", "দাম বাড়ল", "দাম কমল", "পেঁয়াজ", "চাল", "সয়াবিন", "গ্যাস", "বিদ্যুৎ",
        "লোডশেডিং", "জ্বালানি", "ডলার", "রিজার্ভ", "ব্যাংক", "লুটপাট", "খেলাপি ঋণ",
        "গ্রেপ্তার", "আটক", "রিমান্ড", "কারাগার", "জেল", "পরোয়ানা", "আদালত", "হাইকোর্ট",
        "সুপ্রিম কোর্ট", "রায়", "ফাঁসি", "যাবজ্জীবন", "মামলা", "তদন্ত", "র‍্যাব", "পুলিশ",
        "ডিবি", "পিবিআই", "সিআইডি", "বিজিবি", "কোস্টগার্ড", "অভিযান", "উদ্ধার", "জব্দ",
        "অস্ত্র", "মাদক", "ইয়াবা", "হত্যা", "খুন", "লাশ", "মরদেহ", "গুম", "অপহরণ",
        "ধর্ষণ", "নির্যাতন", "প্রতারণা", "জালিয়াতি", "ছিনতাই", "ডাকাতি", "আগুন", "অগ্নিকাণ্ড",
        "বিস্ফোরণ", "ধস", "দুর্ঘটনা", "সড়ক দুর্ঘটনা", "ট্রেন", "লঞ্চ", "নিহত", "আহত", "মৃত্যু",
        "ধর্মঘট", "কর্মবিরতি", "আন্দোলন", "প্রতিবাদ", "বড় সিদ্ধান্ত", "সুখবর", "দুঃসংবাদ",
        "জরুরি", "ব্রেকিং", "সতর্কতা", "ঘূর্ণিঝড়", "বন্যা", "ভূমিকম্প", "প্রবাসী", "রেমিট্যান্স",
        "ভিসা", "পাসপোর্ট", "বিমানবন্দর", "মেট্রোরেল", "পদ্মা সেতু", "সারা দেশে", "সারাদেশে", "দেশজুড়ে"
    )

    private val BD_DOMESTIC_ANCHORS = listOf(
        "বাংলাদেশ", "বাংলাদেশে", "বাংলাদেশের", "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী",
        "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ", "কুমিল্লা", "গাজীপুর", "নারায়ণগঞ্জ",
        "কক্সবাজার", "সাভার", "উত্তরা", "মিরপুর", "ধানমন্ডি", "গুলশান", "শাহবাগ", "মতিঝিল",
        "সচিবালয়", "হাইকোর্ট", "সুপ্রিম কোর্ট", "বিএনপি", "জামায়াত", "আওয়ামী লীগ", "এনসিপি",
        "প্রধান উপদেষ্টা", "অন্তর্বর্তী সরকার", "ডিএমপি", "র‍্যাব", "দুদক", "বিজিবি", "bangladesh", "dhaka"
    )

    /**
     * Quick block check used even when the user searches manually in Live News:
     * Sports and ordinary (non-viral) entertainment are always excluded.
     */
    fun isBlockedFromLiveNews(article: NewsArticle): Boolean {
        val urlLower = article.sourceUrl.lowercase()
        val titleLower = article.title.lowercase()
        val fullText = "${article.title} ${article.summary}".lowercase()

        if (isSports(urlLower, titleLower, fullText)) return true
        if (isEntertainment(urlLower, titleLower) && !hasViralEntertainmentSignal(fullText)) return true
        return false
    }

    /**
     * Classifies a single article into Tab 1 (Bangladesh Politics / Viral / Catchy Nationwide)
     * or Tab 2 (Iran-Israel-Palestine / Hot International), or returns `null` to exclude it.
     */
    fun classify(article: NewsArticle): CuratedLiveArticle? {
        val urlLower = article.sourceUrl.lowercase()
        val titleLower = article.title.lowercase()
        val fullText = "${article.title} ${article.summary}".lowercase()

        // 1. Hard-block ALL Sports
        if (isSports(urlLower, titleLower, fullText)) return null

        // 2. Entertainment: ONLY allow viral / scandalous / breaking entertainment
        if (isEntertainment(urlLower, titleLower)) {
            if (!hasViralEntertainmentSignal(fullText)) return null
            val viralHits = countMatches(fullText, VIRAL_ENTERTAINMENT_SIGNALS)
            return CuratedLiveArticle(
                article = article,
                tab = LiveNewsTab.BANGLADESH_HOT,
                badgeBn = "ভাইরাল বিনোদন",
                priorityScore = 84 + (viralHits * 6).coerceAtMost(24)
            )
        }

        // 3. Check high-priority Middle East (Iran / Israel / Palestine) FIRST
        val hasIran = containsAny(fullText, IRAN_KEYWORDS)
        val hasIsrael = containsAny(fullText, ISRAEL_KEYWORDS)
        val hasPalestine = containsAny(fullText, PALESTINE_KEYWORDS)
        val hasMiddleEastOther = containsAny(fullText, MIDDLE_EAST_OTHER_KEYWORDS)

        if (hasIran || hasIsrael || hasPalestine || hasMiddleEastOther) {
            val dramaHits = countMatches(fullText, INTL_CATCHY_EVENT_KEYWORDS)
            val badge = when {
                hasIran && hasIsrael -> "ইরান-ইসরায়েল"
                hasIran -> "ইরান হট"
                hasPalestine -> "ফিলিস্তিন-গাজা"
                hasIsrael -> "ইসরায়েল"
                else -> "মধ্যপ্রাচ্য"
            }
            val baseScore = when {
                hasIran && hasIsrael -> 155
                hasIran || hasIsrael || hasPalestine -> 142
                else -> 125
            }
            return CuratedLiveArticle(
                article = article,
                tab = LiveNewsTab.INTERNATIONAL,
                badgeBn = badge,
                priorityScore = baseScore + (dramaHits * 10).coerceAtMost(40),
                isHotMiddleEast = true
            )
        }

        // 4. Score Politics, Viral, National Catchy, International
        val politicsUrl = containsAny(urlLower, BD_POLITICS_URL_PATTERNS)
        val politicsHits = countMatches(fullText, BD_POLITICS_KEYWORDS) + if (politicsUrl) 2 else 0
        val viralHits = countMatches(fullText, VIRAL_CATCHY_KEYWORDS)
        val nationalCatchyHits = countMatches(fullText, BD_NATIONAL_CATCHY_KEYWORDS)
        val bdAnchorHits = countMatches(fullText, BD_DOMESTIC_ANCHORS)

        val intlUrl = containsAny(urlLower, INTL_URL_PATTERNS)
        val intlEntityHits = countMatches(fullText, INTL_COUNTRY_LEADER_KEYWORDS)
        val intlDramaHits = countMatches(fullText, INTL_CATCHY_EVENT_KEYWORDS)

        // 5. Block routine/mundane filler unless strong politics or viral signal overrides
        val isRoutine = containsAny(urlLower, ROUTINE_URL_PATTERNS) || containsAny(titleLower, ROUTINE_HEADLINE_PATTERNS)
        if (isRoutine && politicsHits == 0 && viralHits == 0) {
            return null
        }

        // 6. Route International Tab (catchy / important world news only)
        val isPrimarilyInternational =
            (intlUrl && bdAnchorHits == 0 && politicsHits == 0) ||
                (intlEntityHits > 0 && bdAnchorHits == 0 && politicsHits == 0) ||
                (intlUrl && intlEntityHits > 0 && politicsHits == 0)

        if (isPrimarilyInternational) {
            // Require catchy/dramatic/high-interest global angle (no dull foreign filler)
            val isCatchyIntl = intlDramaHits > 0 || viralHits > 0 || intlEntityHits >= 2
            if (!isCatchyIntl) return null
            val badge = if (intlDramaHits >= 2 || intlEntityHits >= 2) "বিশ্ব রাজনীতি" else "আন্তর্জাতিক"
            return CuratedLiveArticle(
                article = article,
                tab = LiveNewsTab.INTERNATIONAL,
                badgeBn = badge,
                priorityScore = 76 + (intlDramaHits * 12).coerceAtMost(36) + (intlEntityHits * 8).coerceAtMost(24) + (viralHits * 10).coerceAtMost(20)
            )
        }

        // 7. Route Bangladesh Tab (Politics > Viral > Catchy Nationwide)
        if (politicsHits > 0) {
            return CuratedLiveArticle(
                article = article,
                tab = LiveNewsTab.BANGLADESH_HOT,
                badgeBn = "রাজনীতি",
                priorityScore = 112 + (politicsHits * 10).coerceAtMost(36) + (viralHits * 12).coerceAtMost(24)
            )
        }

        if (viralHits > 0) {
            return CuratedLiveArticle(
                article = article,
                tab = LiveNewsTab.BANGLADESH_HOT,
                badgeBn = "ভাইরাল",
                priorityScore = 96 + (viralHits * 12).coerceAtMost(30) + (nationalCatchyHits * 6).coerceAtMost(18)
            )
        }

        if (nationalCatchyHits > 0) {
            // Require either strong catchy signal or Bangladesh context so ordinary bland pieces stay out
            if (nationalCatchyHits >= 1 && (bdAnchorHits > 0 || nationalCatchyHits >= 2 || containsAny(titleLower, BD_NATIONAL_CATCHY_KEYWORDS))) {
                return CuratedLiveArticle(
                    article = article,
                    tab = LiveNewsTab.BANGLADESH_HOT,
                    badgeBn = "সারাদেশ",
                    priorityScore = 72 + (nationalCatchyHits * 10).coerceAtMost(30)
                )
            }
        }

        // 8. Everything else is ordinary/general news -> exclude!
        return null
    }

    fun classifyAndSort(articles: List<NewsArticle>, tab: LiveNewsTab): List<CuratedLiveArticle> {
        val nowMs = System.currentTimeMillis()
        return articles.mapNotNull { classify(it) }
            .filter { it.tab == tab }
            .sortedWith(
                compareByDescending<CuratedLiveArticle> { it.priorityScore + recencyBoost(it.article.publishedIso, nowMs) }
                    .thenByDescending { it.article.publishedIso }
            )
    }

    fun classifyAll(articles: List<NewsArticle>): List<CuratedLiveArticle> {
        return articles.mapNotNull { classify(it) }
    }

    fun sortForDisplay(items: List<CuratedLiveArticle>): List<CuratedLiveArticle> {
        val nowMs = System.currentTimeMillis()
        return items.sortedWith(
            compareByDescending<CuratedLiveArticle> { it.priorityScore + recencyBoost(it.article.publishedIso, nowMs) }
                .thenByDescending { it.article.publishedIso }
        )
    }

    private fun isSports(urlLower: String, titleLower: String, fullText: String): Boolean {
        if (containsAny(urlLower, SPORTS_URL_PATTERNS)) return true
        if (containsAny(titleLower, SPORTS_STRONG_KEYWORDS)) return true
        // Secondary check in summary if multiple sports terms appear
        return countMatches(fullText, SPORTS_STRONG_KEYWORDS) >= 2
    }

    private fun isEntertainment(urlLower: String, titleLower: String): Boolean {
        if (containsAny(urlLower, ENTERTAINMENT_URL_PATTERNS)) return true
        return containsAny(titleLower, ENTERTAINMENT_KEYWORDS)
    }

    private fun hasViralEntertainmentSignal(fullText: String): Boolean {
        return containsAny(fullText, VIRAL_ENTERTAINMENT_SIGNALS)
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (text.contains(kw)) return true
        }
        return false
    }

    private fun countMatches(text: String, keywords: List<String>): Int {
        var count = 0
        for (kw in keywords) {
            if (text.contains(kw)) {
                count++
                if (count >= 5) break
            }
        }
        return count
    }

    private fun recencyBoost(publishedIso: String, nowMs: Long): Int {
        val millis = parseIsoMillis(publishedIso) ?: return 0
        val ageHours = ((nowMs - millis) / 3_600_000L).coerceAtLeast(0L)
        return when {
            ageHours <= 3L -> 36
            ageHours <= 8L -> 24
            ageHours <= 24L -> 14
            ageHours <= 48L -> 6
            else -> 0
        }
    }

    private fun parseIsoMillis(iso: String): Long? {
        if (iso.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (p in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                sdf.parse(iso)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }
}
