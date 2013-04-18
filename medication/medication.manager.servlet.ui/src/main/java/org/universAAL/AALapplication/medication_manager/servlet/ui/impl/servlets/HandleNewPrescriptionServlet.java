package org.universAAL.AALapplication.medication_manager.servlet.ui.impl.servlets;

import org.universAAL.AALapplication.medication_manager.persistence.layer.PersistentService;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.PrescriptionDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dto.MedicineDTO;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dto.PrescriptionDTO;
import org.universAAL.AALapplication.medication_manager.persistence.layer.entities.Person;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.Log;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.MedicationManagerServletUIException;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.servlets.helpers.MedicineView;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.servlets.helpers.NewPrescriptionView;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.servlets.helpers.Session;
import org.universAAL.AALapplication.medication_manager.servlet.ui.impl.servlets.helpers.SessionTracking;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.universAAL.AALapplication.medication_manager.servlet.ui.impl.Activator.*;
import static org.universAAL.AALapplication.medication_manager.servlet.ui.impl.Util.*;

/**
 * @author George Fournadjiev
 */
public final class HandleNewPrescriptionServlet extends BaseServlet {

  private final Object lock = new Object();
  private DisplayLoginHtmlWriterServlet displayLoginHtmlWriterServlet;
  private SelectUserHtmlWriterServlet selectUserHtmlWriterServlet;
  private NewPrescriptionHtmlWriterServlet newPrescriptionHtmlWriterServlet;
  private ListPrescriptionsHtmlWriterServlet listPrescriptionsHtmlWriterServlet;

  public HandleNewPrescriptionServlet(SessionTracking sessionTracking) {
    super(sessionTracking);
  }

  public void setDisplayLoginHtmlWriterServlet(DisplayLoginHtmlWriterServlet displayLoginHtmlWriterServlet) {
    this.displayLoginHtmlWriterServlet = displayLoginHtmlWriterServlet;
  }

  public void setSelectUserHtmlWriterServlet(SelectUserHtmlWriterServlet selectUserHtmlWriterServlet) {
    this.selectUserHtmlWriterServlet = selectUserHtmlWriterServlet;
  }

  public void setNewPrescriptionHtmlWriterServlet(NewPrescriptionHtmlWriterServlet newPrescriptionHtmlWriterServlet) {
    this.newPrescriptionHtmlWriterServlet = newPrescriptionHtmlWriterServlet;
  }

  public void setListPrescriptionsHtmlWriterServlet(ListPrescriptionsHtmlWriterServlet listPrescriptionsHtmlWriterServlet) {
    this.listPrescriptionsHtmlWriterServlet = listPrescriptionsHtmlWriterServlet;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    synchronized (lock) {
      try {
        isServletSet(displayLoginHtmlWriterServlet, "displayLoginHtmlWriterServlet");
        isServletSet(selectUserHtmlWriterServlet, "selectUserHtmlWriterServlet");
        isServletSet(listPrescriptionsHtmlWriterServlet, "listPrescriptionsHtmlWriterServlet");
        isServletSet(newPrescriptionHtmlWriterServlet, "newPrescriptionHtmlWriterServlet");
        Session session = getSession(req, resp, getClass());
        Person doctor = (Person) session.getAttribute(LOGGED_DOCTOR);

        if (doctor == null) {
          debugSessions(session.getId(), "if(doctor is null) the servlet doGet/doPost method", getClass());
          displayLoginHtmlWriterServlet.doGet(req, resp);
          return;
        }

        Person patient = (Person) session.getAttribute(PATIENT);

        if (patient == null) {
          debugSessions(session.getId(), "if(patient is null) the servlet doGet/doPost method", getClass());
          selectUserHtmlWriterServlet.doGet(req, resp);
          return;
        }

        NewPrescriptionView newPrescriptionView = (NewPrescriptionView) session.getAttribute(PRESCRIPTION_VIEW);

        if (newPrescriptionView == null) {
          debugSessions(session.getId(), "if(newPrescriptionView is null) the servlet doGet/doPost method", getClass());
          newPrescriptionHtmlWriterServlet.doGet(req, resp);
          return;
        }

        PersistentService persistentService = getPersistentService();

        checkForNotesAndDateParameters(req, newPrescriptionView);
        PrescriptionDTO prescriptionDTO = saveNewPrescription(newPrescriptionView, req, resp, persistentService);

        if (prescriptionDTO != null) {
          session.removeAttribute(PRESCRIPTION_VIEW);
          PrescriptionDao prescriptionDao = persistentService.getPrescriptionDao();
          prescriptionDao.save(prescriptionDTO);
        }

        debugSessions(session.getId(), "End of the servlet doGet/doPost method", getClass());
        listPrescriptionsHtmlWriterServlet.doPost(req, resp);
      } catch (Exception e) {
        Log.error(e.fillInStackTrace(), "Unexpected Error occurred", getClass());
        sendErrorResponse(req, resp, e);
      }

    }
  }

  private void checkForNotesAndDateParameters(HttpServletRequest req, NewPrescriptionView newPrescriptionView) {
    String date = req.getParameter(DATE);
    if (date != null && !date.trim().isEmpty()) {
      newPrescriptionView.setStartDate(date);
    }

    String notes = req.getParameter(NOTES);
    if (notes != null && !notes.trim().isEmpty()) {
      notes = escapeNewLinesAndSingleQuotes(notes);
      newPrescriptionView.setNotes(notes);
    }
  }

  private PrescriptionDTO saveNewPrescription(NewPrescriptionView prescriptionView, HttpServletRequest req,
                                              HttpServletResponse resp, PersistentService persistentService) {

    Log.info("Creating PrescriptionDTO object from NewPrescriptionView object: %s", getClass(), prescriptionView);

    Date startDate = getDate(prescriptionView.getStartDate());
    PrescriptionDTO prescriptionDTO = new PrescriptionDTO(
        prescriptionView.getNotes(),
        startDate,
        getMedicineDTOs(prescriptionView.getMedicineViewSet(), startDate, persistentService),
        prescriptionView.getPhysician(),
        prescriptionView.getPatient()
    );

    int prescriptionId = persistentService.generateId();
    prescriptionDTO.setPrescriptionId(prescriptionId);

    return prescriptionDTO;
  }

  private Set<MedicineDTO> getMedicineDTOs(Set<MedicineView> medicineViewSet,
                                           Date startDate, PersistentService persistentService) {

    Set<MedicineDTO> medicineDTOSet = new HashSet<MedicineDTO>();

    if (medicineViewSet.isEmpty()) {
      throw new MedicationManagerServletUIException("The medicineViewSet cannot be empty");
    }

    for (MedicineView medicineView : medicineViewSet) {
      MedicineDTO medicineDTO = createMedicineDTO(medicineView, startDate, persistentService);
      medicineDTOSet.add(medicineDTO);
    }

    return medicineDTOSet;
  }

  private MedicineDTO createMedicineDTO(MedicineView medicineView, Date startDate,
                                        PersistentService persistentService) {

    MedicineDTO medicineDTO = new MedicineDTO(
        medicineView.getName(),
        startDate,
        medicineView.getDays(),
        medicineView.getDescription(),
        medicineView.getSideeffects(),
        medicineView.getIncompliances(),
        medicineView.getMealRelationDTO(),
        medicineView.getIntakeDTOSet()
    );
    medicineDTO.setMedicineIdId(persistentService.generateId());

    return medicineDTO;
  }
}
