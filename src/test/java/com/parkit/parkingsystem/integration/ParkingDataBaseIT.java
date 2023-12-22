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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        try { // Stubbing inputReaderUtil and dataBasePrepareService
            lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            dataBasePrepareService.clearDataBaseEntries();

            // Set up the ParkingService
            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

            // Stubbing the call to getNextParkingNumberIfAvailable
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            when(parkingSpotDAO.getNextAvailableSlot(any())).thenReturn(parkingSpot.getId()); // Retourne l'ID du
                                                                                              // parking
                                                                                              // spot

            // Stubbing the call to ticketDAO.saveTicket
            doNothing().when(ticketDAO).saveTicket(any());

            // Call the method to be tested
            parkingService.processIncomingVehicle();

            // Verify that the saveTicket method was called with a non-null argument
            verify(ticketDAO, times(1)).saveTicket(any());

            // Other assertions as needed
            // For example, you can verify that the parkingSpotDAO.updateParking method was
            // called
            verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);

        } catch (

        Exception e) {
            // Gére l'exception (affichage de logs, etc.)
            e.printStackTrace();
        }
        // TODO: check that a ticket is actualy saved in DB and Parking table is updated
        // with availability
    }

    @Test
    public void testParkingLotExit() {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        // Mock the behavior of getNbTicket to return a specific value
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);

        parkingService.processExitingVehicle();

        // TODO: check that the fare generated and out time are populated correctly in
        // the database
    }

}
