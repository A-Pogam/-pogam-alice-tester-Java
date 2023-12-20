package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    @InjectMocks
    private ParkingService parkingService;

    @Mock
    private InputReaderUtil inputReaderUtil;

    @Mock
    private ParkingSpotDAO parkingSpotDAO;

    @Mock
    private TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }

    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        // Arrange
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // Ensure that updateTicket returns true
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void processIncomingVehicle() {
        // Arrange
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(1);

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        // Arrange
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        // Stubbing pour ticketDAO.getTicket(anyString())
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

        // Stubbing pour ticketDAO.updateTicket(any(Ticket.class)) Assurez-vous que
        // updateTicket renvoie false pour simuler l'échec de la mise à jour
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // Utilisation de lenient() pour rendre l'interaction avec updateParking non
        // requise
        lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Act
        parkingService.processExitingVehicle();

        // Assert Vérifie que ticketDAO.updateTicket a été appelé une fois avec
        // n'importe quelle instance de Ticket
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        // Vérifie que parkingSpotDAO.updateParking n'a pas été appelé
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        // Act
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(ParkingType.CAR, result.getParkingType());
        assertTrue(result.isAvailable());

        // Verify interactions with mocked objects
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verifyNoMoreInteractions(parkingSpotDAO);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        // Act
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNull(parkingSpot);

        // Verify interactions with mocked objects
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verifyNoMoreInteractions(parkingSpotDAO);
    }

}
