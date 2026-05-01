package com.project.flightservice.service;

import com.project.flightservice.exception.*;
import com.project.flightservice.kafka.FlightEventProducer;
import com.project.flightservice.kafka.event.*;
import com.project.flightservice.model.*;
import com.project.flightservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository       flightRepository;
    private final FlightRouteRepository  routeRepository;
    private final FlightEventProducer    eventProducer;
    private final GraphBuilderService    graphBuilder;

    @Transactional
    public Flight updateFlightDelay(UUID flightId, int delayMinutes, String reason) {
        Flight flight = findOrThrow(flightId);

        validateDelayUpdate(flight, delayMinutes);

        Instant newDeparture = flight.getScheduledDeparture()
                .plusSeconds(delayMinutes * 60L);

        flightRepository.updateFlightStatus(
                flightId, FlightStatus.DELAYED, delayMinutes, reason, newDeparture
        );

        graphBuilder.invalidateFlightEdges(flightId);

        eventProducer.publishFlightDelayed(FlightDelayedEvent.builder()
                .flightId(flightId.toString())
                .flightNumber(flight.getFlightNumber())
                .airlineCode(flight.getAirlineCode())
                .originIata(flight.getOriginAirport().getIataCode())
                .destIata(flight.getDestAirport().getIataCode())
                .originalDeparture(flight.getScheduledDeparture())
                .newDeparture(newDeparture)
                .delayMinutes(delayMinutes)
                .reason(reason)
                .build());

        log.info("Flight {} delayed by {}min — graph edges invalidated", flightId, delayMinutes);
        return flight;
    }

    @Transactional
    public void cancelFlight(UUID flightId, String reason) {
        Flight flight = findOrThrow(flightId);

        if (flight.getStatus() == FlightStatus.DEPARTED
                || flight.getStatus() == FlightStatus.ARRIVED) {
            throw new InvalidFlightOperationException(
                    "Cannot cancel a flight that has already departed or arrived");
        }

        flightRepository.updateFlightStatus(
                flightId, FlightStatus.CANCELLED, 0, reason, null
        );

        routeRepository.deactivateByFlightId(flightId);
        graphBuilder.invalidateFlightEdges(flightId);

        eventProducer.publishFlightCancelled(FlightCancelledEvent.builder()
                .flightId(flightId.toString())
                .flightNumber(flight.getFlightNumber())
                .airlineCode(flight.getAirlineCode())
                .originIata(flight.getOriginAirport().getIataCode())
                .destIata(flight.getDestAirport().getIataCode())
                .scheduledDeparture(flight.getScheduledDeparture())
                .cancellationReason(reason)
                .build());

        log.info("Flight {} cancelled — edges deactivated, graph cache invalidated", flightId);
    }

    @Transactional(readOnly = true)
    public Flight findOrThrow(UUID id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found: " + id));
    }


    private void validateDelayUpdate(Flight flight, int delayMinutes) {
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new InvalidFlightOperationException(
                    "Cannot delay a cancelled flight");
        }
        if (flight.getStatus() == FlightStatus.ARRIVED) {
            throw new InvalidFlightOperationException(
                    "Cannot delay a flight that has already arrived");
        }
        if (delayMinutes < 0) {
            throw new InvalidFlightOperationException(
                    "Delay minutes cannot be negative");
        }
        if (delayMinutes > 1440) {  
            throw new InvalidFlightOperationException(
                    "Delay exceeds 24 hours — please reschedule the flight instead");
        }
    }
}
