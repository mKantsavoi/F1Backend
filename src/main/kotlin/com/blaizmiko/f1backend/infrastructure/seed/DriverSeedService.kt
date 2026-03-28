package com.blaizmiko.f1backend.infrastructure.seed

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class PhotoMapping(
    val team: String,
    val code: String,
)

@Serializable
private data class BiographyEntry(
    val driverId: String,
    val biography: String,
)

@Serializable
private data class BiographiesFile(
    val drivers: List<BiographyEntry>,
)

class DriverSeedService(
    private val driverRepository: DriverRepository,
    private val teamRepository: TeamRepository,
    private val teamDataSource: TeamDataSource,
    private val driverDataSource: DriverDataSource,
    private val standingsDataSource: StandingsDataSource,
) {
    private val logger = LoggerFactory.getLogger(DriverSeedService::class.java)

    companion object {
        private const val PHOTO_BASE =
            "https://media.formula1.com/image/upload" +
                "/c_fill,w_720/q_auto/v1740000001/common/f1/2026"

        private fun buildPhotoUrl(
            team: String,
            code: String,
        ): String = "$PHOTO_BASE/$team/$code/2026$team${code}right.webp"
    }

    suspend fun seedIfEmpty() {
        if (driverRepository.count() > 0L) {
            logger.info("Drivers table not empty, skipping seed")
            return
        }
        logger.info("Drivers table empty, starting seed...")
        seed()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun seed() {
        val (_, teams) = teamDataSource.fetchTeams("current")
        teamRepository.insertAll(teams)
        logger.info("Seeded {} teams", teams.size)

        val (_, baseDrivers) = driverDataSource.fetchDrivers("current")
        val baseMap = baseDrivers.associateBy { it.id }

        val standings = standingsDataSource.fetchDriverStandings("current")
        val driverTeamMap = standings.standings.associate { it.driverId to it.teamId }

        val photoMap = loadPhotoMapping()
        val bioMap = loadBiographies()

        val drivers =
            standings.standings.map { standing ->
                val base = baseMap[standing.driverId]
                val photoUrl =
                    photoMap[standing.driverId]?.let { mapping ->
                        buildPhotoUrl(mapping.team, mapping.code)
                    }
                Driver(
                    id = standing.driverId,
                    number = base?.number ?: 0,
                    code = base?.code ?: standing.driverCode,
                    firstName = base?.firstName ?: standing.driverName.substringBefore(" "),
                    lastName = base?.lastName ?: standing.driverName.substringAfter(" "),
                    nationality = base?.nationality ?: standing.nationality,
                    dateOfBirth = base?.dateOfBirth,
                    photoUrl = photoUrl,
                    teamId = driverTeamMap[standing.driverId],
                    biography = bioMap[standing.driverId],
                )
            }

        driverRepository.insertAll(drivers)
        val photosApplied = drivers.count { it.photoUrl != null }
        val biosApplied = drivers.count { it.biography != null }
        logger.info(
            "Seeded {} drivers ({} with photos, {} with biographies)",
            drivers.size,
            photosApplied,
            biosApplied,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadPhotoMapping(): Map<String, PhotoMapping> =
        try {
            val text =
                this::class.java.classLoader
                    .getResource("seed/driver-photos.json")
                    ?.readText()
            if (text != null) {
                json.decodeFromString<Map<String, PhotoMapping>>(text)
            } else {
                logger.warn("Photo mapping file not found, continuing without photos")
                emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to load photo mapping: {}", e.message)
            emptyMap()
        }

    @Suppress("TooGenericExceptionCaught")
    private fun loadBiographies(): Map<String, String> =
        try {
            val text =
                this::class.java.classLoader
                    .getResource("seed/driver-biographies.json")
                    ?.readText()
            if (text != null) {
                val file = json.decodeFromString<BiographiesFile>(text)
                file.drivers.associate { it.driverId to it.biography }
            } else {
                logger.warn("Biographies file not found, continuing without biographies")
                emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to load biographies: {}", e.message)
            emptyMap()
        }
}
