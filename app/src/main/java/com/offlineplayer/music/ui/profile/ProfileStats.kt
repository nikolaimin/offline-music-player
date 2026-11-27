package com.offlineplayer.music.ui.profile

import com.offlineplayer.music.data.PlayHistoryEntry
import com.offlineplayer.music.data.Track

// Так MusicRepository подписывает файлы без тегов: в «любимые» такие значения не попадают.
private const val UnknownArtist = "Неизвестный исполнитель"
private const val UnknownAlbum = "Неизвестный альбом"

/** Сколько недавно прослушанных треков показывает профиль. */
const val RecentShown = 10

/** Числа для блока «Моя статистика». Всё считается из данных приложения. */
data class ProfileStats(
    val totalPlays: Int = 0,
    val trackCount: Int = 0,
    val favoriteCount: Int = 0,
    val playlistCount: Int = 0
)

data class TopTrack(val track: Track, val plays: Int)

/** Любимый исполнитель: [coverTrackId] — самый прослушиваемый его трек (его обложка). */
data class FavoriteArtist(val name: String, val plays: Int, val coverTrackId: Long)

data class FavoriteAlbum(val name: String, val artist: String, val plays: Int, val coverTrackId: Long)

data class RecentPlay(val track: Track, val playedAtMs: Long)

/** Достижение: условие и прогресс считаются из [ProfileStats]. */
enum class Achievement(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val target: Int,
    private val value: (ProfileStats) -> Int
) {
    FirstPlay("first_play", "▶", "Первое прослушивание", "Прослушайте свой первый трек", 1, { it.totalPlays }),
    HundredPlays("hundred_plays", "♪", "100 прослушиваний", "Прослушайте 100 треков", 100, { it.totalPlays }),
    MusicCollector("music_collector", "♥", "Коллекционер", "Добавьте 50 треков в избранное", 50, { it.favoriteCount }),
    PlaylistCreator("playlist_creator", "＋", "Создатель плейлистов", "Создайте первый плейлист", 1, { it.playlistCount }),
    PlaylistMaster("playlist_master", "♛", "Мастер плейлистов", "Создайте 10 плейлистов", 10, { it.playlistCount });

    /** Текущее значение для условия (не больше цели: для полосы прогресса). */
    fun progress(stats: ProfileStats): Int = value(stats).coerceIn(0, target)

    fun isReached(stats: ProfileStats): Boolean = value(stats) >= target
}

data class AchievementUi(
    val achievement: Achievement,
    val progress: Int,
    /** Когда получено, или null, если ещё нет. */
    val unlockedAtMs: Long?
)

data class ProfileDetails(
    val stats: ProfileStats = ProfileStats(),
    val topTracks: List<TopTrack> = emptyList(),
    val favoriteArtist: FavoriteArtist? = null,
    val favoriteAlbum: FavoriteAlbum? = null,
    val recent: List<RecentPlay> = emptyList(),
    val achievements: List<AchievementUi> = emptyList()
)

/**
 * Собирает данные профиля из библиотеки (уже с правками пользователя), счётчиков прослушиваний,
 * истории и списка полученных достижений. Ничего не выдумывает: нет данных — пустые списки и null.
 */
fun buildProfileDetails(
    library: List<Track>,
    counts: Map<Long, Int>,
    totalPlays: Int,
    favoriteCount: Int,
    playlistCount: Int,
    history: List<PlayHistoryEntry>,
    unlocked: Map<String, Long>
): ProfileDetails {
    val stats = ProfileStats(
        totalPlays = totalPlays,
        trackCount = library.size,
        favoriteCount = favoriteCount,
        playlistCount = playlistCount
    )

    // Только прослушанные треки, которые ещё есть на телефоне: самые частые первыми.
    val played = library
        .filter { (counts[it.id] ?: 0) > 0 }
        .sortedWith(compareByDescending<Track> { counts[it.id] ?: 0 }.thenBy { it.title.lowercase() })

    val topTracks = played.take(5).map { TopTrack(it, counts[it.id] ?: 0) }

    val favoriteArtist = played
        .filter { it.artist != UnknownArtist }
        .groupBy { it.artist }
        .map { (name, tracks) -> Triple(name, tracks.sumOf { counts[it.id] ?: 0 }, tracks.first()) }
        .maxWithOrNull(compareBy<Triple<String, Int, Track>> { it.second }.thenByDescending { it.first })
        ?.let { (name, plays, top) -> FavoriteArtist(name, plays, top.id) }

    val favoriteAlbum = played
        .filter { it.album != UnknownAlbum }
        .groupBy { it.album }
        .map { (name, tracks) -> Triple(name, tracks.sumOf { counts[it.id] ?: 0 }, tracks.first()) }
        .maxWithOrNull(compareBy<Triple<String, Int, Track>> { it.second }.thenByDescending { it.first })
        ?.let { (name, plays, top) -> FavoriteAlbum(name, top.artist, plays, top.id) }

    // Каждый трек один раз, с последним временем прослушивания; удалённые с телефона пропускаем.
    val byId = library.associateBy { it.id }
    val recent = history
        .distinctBy { it.trackId }
        .mapNotNull { entry -> byId[entry.trackId]?.let { RecentPlay(it, entry.playedAtMs) } }
        .take(RecentShown)

    val achievements = Achievement.entries.map {
        AchievementUi(it, it.progress(stats), unlocked[it.id])
    }

    return ProfileDetails(stats, topTracks, favoriteArtist, favoriteAlbum, recent, achievements)
}
