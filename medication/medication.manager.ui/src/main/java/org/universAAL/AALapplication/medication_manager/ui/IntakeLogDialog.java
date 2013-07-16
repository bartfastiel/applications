/*******************************************************************************
 * Copyright 2012 , http://www.prosyst.com - ProSyst Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package org.universAAL.AALapplication.medication_manager.ui;

import org.universAAL.AALapplication.medication_manager.persistence.layer.IntakeInfo;
import org.universAAL.AALapplication.medication_manager.persistence.layer.PersistentService;
import org.universAAL.AALapplication.medication_manager.persistence.layer.Week;
import org.universAAL.AALapplication.medication_manager.persistence.layer.dao.ComplexDao;
import org.universAAL.AALapplication.medication_manager.persistence.layer.entities.Person;
import org.universAAL.AALapplication.medication_manager.ui.impl.Log;
import org.universAAL.AALapplication.medication_manager.ui.impl.MedicationManagerUIException;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.owl.supply.LevelRating;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.ui.UICaller;
import org.universAAL.middleware.ui.UIRequest;
import org.universAAL.middleware.ui.UIResponse;
import org.universAAL.middleware.ui.owl.PrivacyLevel;
import org.universAAL.middleware.ui.rdf.Form;
import org.universAAL.middleware.ui.rdf.Label;
import org.universAAL.middleware.ui.rdf.SimpleOutput;
import org.universAAL.middleware.ui.rdf.Submit;
import org.universAAL.ontology.profile.User;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import static org.universAAL.AALapplication.medication_manager.ui.impl.Activator.*;

public class IntakeLogDialog extends UICaller {

  private final ModuleContext moduleContext;
  private Week currentWeek;
  private final PersistentService persistentService;
  private final Person patient;
  private User currentUser; //TODO to be removed (hack for saied user)
  private boolean userActed;

  private static final String CLOSE_BUTTON = "closeButton";
  private static final String PREV_BUTTON = "previousWeekButton";
  private static final String NEXT_BUTTON = "nextWeekButton";

  public IntakeLogDialog(ModuleContext context, PersistentService persistentService,
                         Week currentWeek, Person patient) {

    super(context);

    validateParameter(context, "context");
    validateParameter(persistentService, "persistentService");
    validateParameter(currentWeek, "currentWeek");

    moduleContext = context;
    this.userActed = false;
    this.currentWeek = currentWeek;
    this.persistentService = persistentService;
    this.patient = patient;
  }

  @Override
  public void communicationChannelBroken() {
  }

  @Override
  public void dialogAborted(String dialogID) {
  }

  @Override
  public void handleUIResponse(UIResponse input) {
    try {
      userActed = true;
      if (CLOSE_BUTTON.equals(input.getSubmissionID())) {
        System.out.println("close");
      } else if (PREV_BUTTON.equals(input.getSubmissionID())) {
        //TODO to be removed (hack for saied user)
        User user = (User) input.getUser();
        if (user.getURI().equals(SAIED.getURI())) {
          user = currentUser;
        }
        showPrevWeekDialog(user);
      } else if (NEXT_BUTTON.equals(input.getSubmissionID())) {
        //TODO to be removed (hack for saied user)
        User user = (User) input.getUser();
        if (user.getURI().equals(SAIED.getURI())) {
          user = currentUser;
        }
        showNextWeekDialog(user);
      } else {
        throw new MedicationManagerUIException("unknown UIResponse");
      }
    } catch (Exception e) {
      Log.error(e, "Error while handling UI response", getClass());
    }

  }

  private void showNextWeekDialog(User user) {

    Calendar currentWeekStartDate = Calendar.getInstance();
    currentWeekStartDate.setTimeInMillis(currentWeek.getBegin().getTime());
    currentWeekStartDate.add(Calendar.DAY_OF_YEAR, 7);
    currentWeek = Week.createWeek(currentWeekStartDate);

    IntakeLogDialog dialog = new IntakeLogDialog(moduleContext, persistentService, currentWeek, patient);
    dialog.showDialog(user);

  }


  private void showPrevWeekDialog(User user) {

    Calendar currentWeekStartDate = Calendar.getInstance();
    currentWeekStartDate.setTimeInMillis(currentWeek.getBegin().getTime());
    currentWeekStartDate.add(Calendar.DAY_OF_YEAR, -7);
    currentWeek = Week.createWeek(currentWeekStartDate);

    IntakeLogDialog dialog = new IntakeLogDialog(moduleContext, persistentService, currentWeek, patient);
    dialog.showDialog(user);

  }

  public void showDialog(User inputUser) {

    try {
      validateParameter(inputUser, "inputUser");

      //TODO to be removed (hack for saied user)
      currentUser = inputUser;

      Form f = Form.newDialog("Medication Manager UI", new Resource());

      //start of the form model


      String message = getMessage(inputUser);

      new SimpleOutput(f.getIOControls(), null, null, message);
      //...
      new Submit(f.getSubmits(), new Label("Close", null), CLOSE_BUTTON);
      new Submit(f.getSubmits(), new Label("Previous Week", null), PREV_BUTTON);
      new Submit(f.getSubmits(), new Label("Next Week", null), NEXT_BUTTON);
      //stop of form model
      //TODO to remove SAIED user and to return inputUser variable
      UIRequest req = new UIRequest(SAIED, f, LevelRating.none, Locale.ENGLISH, PrivacyLevel.insensible);
      this.sendUIRequest(req);
    } catch (Exception e) {
      Log.error(e, "Error while trying to show dialog", getClass());
    }
  }

  private String getMessage(User inputUser) {
    StringBuffer sb = new StringBuffer();
    sb.append(getUserfriendlyName(inputUser));
    sb.append(",\nIntake log/plan for the following week: ");
    sb.append(currentWeek);

    ComplexDao complexDao = persistentService.getComplexDao();
    Set<IntakeInfo> intakeInfos = complexDao.getIntakeInfos(patient, currentWeek);
    String intakeLog = createIntakeLogText(intakeInfos);
    sb.append('\n');
    sb.append(intakeLog);

    return sb.toString();
  }

  public static String getUserfriendlyName(User inputUser) {
    String fullUserUriName = inputUser.toString();
    int index = fullUserUriName.lastIndexOf('#');
    if (index == -1) {
      throw new MedicationManagerUIException("Expected # symbol in the user.getUri() format like: \n" +
          "urn:org.universAAL.aal_space:test_env#saied");
    }
    String firstLetter = fullUserUriName.substring(index + 1).toUpperCase();
    return firstLetter.charAt(0) + fullUserUriName.substring(index + 2);
  }

  public boolean isUserActed() {
    return userActed;
  }

  private String createIntakeLogText(Set<IntakeInfo> intakeInfos) {
    StringBuffer sb = new StringBuffer();

    sb.append('\n');

    int counter = 0;
    for (IntakeInfo intakeInfo : intakeInfos) {
      String row = createRow(intakeInfo);
      Log.info("Added single medicineInventory row: %s", getClass(), row);
      sb.append("\n\n\t");
      counter++;
      sb.append(counter);
      sb.append(". ");
      sb.append(row);
    }

    if (counter == 0) {
      sb.append("There is no intake log/plan for that week");
    }

    sb.append('\n');


    return sb.toString();
  }

  private String createRow(IntakeInfo intakeInfo) {
    StringBuffer sb = new StringBuffer();

    sb.append("Date: ");
    sb.append(intakeInfo.getDate());
    sb.append(", time: ");
    sb.append(intakeInfo.getTime());
    sb.append(", medication: ");
    sb.append(intakeInfo.getMedication());
    sb.append(", status: ");
    sb.append(intakeInfo.getStatus());

    return sb.toString();
  }
}
