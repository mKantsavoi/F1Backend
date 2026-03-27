package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.CircuitDto
import com.blaizmiko.f1backend.adapter.dto.CircuitsResponse
import com.blaizmiko.f1backend.usecase.GetCircuits
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.circuitRoutes() {
    val getCircuits by inject<GetCircuits>()

    get("/circuits") {
        val result = getCircuits.execute()

        if (result.isStale) {
            call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
        }

        call.respond(
            CircuitsResponse(
                circuits =
                    result.circuits.map { circuit ->
                        CircuitDto(
                            circuitId = circuit.id,
                            name = circuit.name,
                            locality = circuit.locality,
                            country = circuit.country,
                            lat = circuit.lat,
                            lng = circuit.lng,
                            url = circuit.url,
                        )
                    },
            ),
        )
    }
}
