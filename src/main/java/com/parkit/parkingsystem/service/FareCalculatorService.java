package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        // Calculating duration in hours
        double duration = (outTimeMillis - inTimeMillis) / (60.0 * 60.0 * 1000.0);

        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                if (duration < 0.5) {
                    ticket.setPrice(0);
                } else {
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                }
                break;
            }
            case BIKE: {
                if (duration < 0.5) {
                    ticket.setPrice(0);
                } else {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}