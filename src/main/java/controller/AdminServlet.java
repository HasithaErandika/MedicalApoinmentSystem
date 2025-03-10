package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Appointment;
import service.AppointmentService;
import service.FileHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminServlet extends HttpServlet {
    private AppointmentService appointmentService;
    private FileHandler doctorFileHandler;
    private FileHandler patientFileHandler;

    @Override
    public void init() throws ServletException {
        String basePath = getServletContext().getRealPath("/data/");
        System.out.println("Base Path: " + basePath); // Debug path
        appointmentService = new AppointmentService(basePath + "appointments.txt");
        doctorFileHandler = new FileHandler(basePath + "doctors.txt");
        patientFileHandler = new FileHandler(basePath + "patients.txt");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = (String) request.getSession().getAttribute("username");
        String role = (String) request.getSession().getAttribute("role");
        if (username == null || !"admin".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/pages/login.jsp?role=admin");
            return;
        }

        try {
            List<Appointment> appointments = appointmentService.readAppointments();
            System.out.println("Raw Appointments: " + (appointments != null ? appointments : "null")); // Debug raw data

            int totalAppointments = getTotalAppointments();
            int totalDoctors = getTotalDoctors();
            int totalPatients = getTotalPatients();
            int emergencyQueueSize = getEmergencyQueueSize();
            List<Appointment> sortedAppointments = getSortedAppointments();

            System.out.println("Total Appointments: " + totalAppointments);
            System.out.println("Total Doctors: " + totalDoctors);
            System.out.println("Total Patients: " + totalPatients);
            System.out.println("Emergency Queue Size: " + emergencyQueueSize);
            System.out.println("Sorted Appointments: " + (sortedAppointments != null ? sortedAppointments : "null"));

            request.setAttribute("totalAppointments", totalAppointments);
            request.setAttribute("totalDoctors", totalDoctors);
            request.setAttribute("totalPatients", totalPatients);
            request.setAttribute("emergencyQueueSize", emergencyQueueSize);
            request.setAttribute("sortedAppointments", sortedAppointments);
        } catch (IOException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error fetching dashboard data: " + e.getMessage());
        }

        request.getRequestDispatcher("/pages/adminDashboard.jsp").forward(request, response);
    }

    private int getTotalAppointments() throws IOException {
        List<Appointment> appointments = appointmentService.readAppointments();
        return appointments != null ? appointments.size() : 0;
    }

    private int getTotalDoctors() throws IOException {
        List<String> doctors = doctorFileHandler.readLines();
        return doctors != null ? doctors.size() : 0;
    }

    private int getTotalPatients() throws IOException {
        List<String> patients = patientFileHandler.readLines();
        return patients != null ? patients.size() : 0;
    }

    private int getEmergencyQueueSize() throws IOException {
        List<Appointment> appointments = appointmentService.readAppointments();
        if (appointments == null) return 0;
        int count = 0;
        for (Appointment appt : appointments) {
            if (appt.getPriority() == 1) count++;
        }
        return count;
    }

    private List<Appointment> getSortedAppointments() throws IOException {
        List<Appointment> appointments = appointmentService.readAppointments();
        if (appointments == null || appointments.isEmpty()) {
            System.out.println("No appointments to sort.");
            return new ArrayList<>();
        }
        int n = appointments.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (appointments.get(j).getDateTime().compareTo(appointments.get(j + 1).getDateTime()) > 0) {
                    Appointment temp = appointments.get(j);
                    appointments.set(j, appointments.get(j + 1));
                    appointments.set(j + 1, temp);
                }
            }
        }
        return appointments;
    }
}