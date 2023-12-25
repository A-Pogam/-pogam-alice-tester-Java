package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        try {
            // Stubbing inputReaderUtil and dataBasePrepareService
            lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            dataBasePrepareService.clearDataBaseEntries();

            // Set up the ParkingService
            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

            // Stubbing the call to getNextParkingNumberIfAvailable
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            when(parkingSpotDAO.getNextAvailableSlot(any())).thenReturn(parkingSpot.getId());

            // Stubbing the call to ticketDAO.saveTicket
            doNothing().when(ticketDAO).saveTicket(any());

            // Call the method to be tested
            parkingService.processIncomingVehicle();

            // Verify that the saveTicket method was called with a non-null argument
            verify(ticketDAO, times(1)).saveTicket(any());

            // Other assertions as needed
            verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);

            // Assert that the ticket is saved in the database
            Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
            assertNotNull(savedTicket);
            assertEquals("ABCDEF", savedTicket.getVehicleRegNumber());

            // Assert that the parking spot is updated in the database
            ParkingSpot updatedParkingSpot = parkingSpotDAO.getParkingSpot(parkingSpot.getId());
            assertNotNull(updatedParkingSpot);
            assertFalse(updatedParkingSpot.isAvailable());

        } catch (Exception e) {
            // Handle the exception (logging, etc.)
            e.printStackTrace();
        }

        // TODO: check that a ticket is actualy saved in DB and Parking table is updated
        // with availability
    }

    @Test
    public void testParkingLotExit() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // Retrieve the incoming vehicle ticket
        Ticket incomingVehicleTicket = ticketDAO.getTicket("ABCDEF");

        if (incomingVehicleTicket != null) {
            System.out.println("Incoming vehicle ticket found: " + incomingVehicleTicket.getId());
            System.out.println("In-time: " + incomingVehicleTicket.getInTime());

            // Adjust the date manipulation logic as needed
            Date theHour = new Date(60 * 60 * 1000);
            Date theHourBefore = new Date(incomingVehicleTicket.getInTime().getTime() - theHour.getTime());
            incomingVehicleTicket.setInTime(theHourBefore);

            // Update the inTime of the ticket in the database
            try (Connection conn = ticketDAO.dataBaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement("UPDATE ticket SET IN_TIME = ? WHERE ID = ?")) {
                pstmt.setTimestamp(1, new Timestamp(incomingVehicleTicket.getInTime().getTime()));
                pstmt.setInt(2, incomingVehicleTicket.getId());
                pstmt.execute();
            } catch (Exception er) {
                er.printStackTrace();
                throw new RuntimeException("Failed to update test ticket with earlier inTime value");
            }

            parkingService.processExitingVehicle();

            // Retrieve the exiting vehicle ticket
            Ticket exitingVehicleTicket = ticketDAO.getTicket("ABCDEF");

            if (exitingVehicleTicket != null) {
                System.out.println("Exiting vehicle ticket found: " + exitingVehicleTicket.getId());
                System.out.println("Out-time: " + exitingVehicleTicket.getOutTime());
                System.out.println("Price: " + exitingVehicleTicket.getPrice());
            } else {
                System.out.println("Exiting vehicle ticket is null");
            }

            assertNotNull(exitingVehicleTicket);
            assertNotNull(exitingVehicleTicket.getOutTime());
            assertNotNull(exitingVehicleTicket.getPrice());

            // TODO: Add assertions for fare and out time in the database
        }
    }

}
