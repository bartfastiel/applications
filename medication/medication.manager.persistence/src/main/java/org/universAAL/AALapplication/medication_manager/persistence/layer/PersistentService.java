package org.universAAL.AALapplication.medication_manager.persistence.layer;

import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.DispenserDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.IntakeDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.InventoryLogDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.MedicineDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.MedicineInventoryDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.PersonDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.PrescriptionDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.TreatmentDao;

/**
 * @author George Fournadjiev
 */
public interface PersistentService {


  SqlUtility getSqlUtility();

  DispenserDao getDispenserDao();

  IntakeDao getIntakeDao();

  InventoryLogDao getInventoryLogDao();

  MedicineDao getMedicineDao();

  MedicineInventoryDao getMedicineInventoryDao();

  PersonDao getPersonDao();

  PrescriptionDao getPrescriptionDao();

  TreatmentDao getTreatmentDao();

}
