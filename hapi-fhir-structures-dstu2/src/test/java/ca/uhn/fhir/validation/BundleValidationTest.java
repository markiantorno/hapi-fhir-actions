package ca.uhn.fhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Appointment;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.valueset.AppointmentStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipantTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipationStatusEnum;
import ca.uhn.fhir.rest.server.CompartmentDstu2Test;
import ca.uhn.fhir.util.TestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Bill de Beaubien on 11/30/2015.
 */
public class BundleValidationTest {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BundleValidationTest.class);
	private static FhirContext ourCtx = FhirContext.forDstu2();

	@Test
	public void testAppointmentIsInvalid() throws Exception {
		Appointment appointment = createAppointment();

		FhirValidator validator = ourCtx.newValidator();
		validator.setValidateAgainstStandardSchema(true);
		validator.setValidateAgainstStandardSchematron(true);

		ValidationResult result = validator.validateWithResult(appointment);
		assertFalse(result.isSuccessful());
		assertThat(result.getMessages()).hasSize(1);
		for (SingleValidationMessage singleValidationMessage : result.getMessages()) {
			ourLog.info(singleValidationMessage.getMessage());
		}
	}

	@Disabled
	@Test
	public void testBundleIsInvalid() throws Exception {
		Appointment appointment = createAppointment();
		Bundle bundle = new Bundle().setType(BundleTypeEnum.TRANSACTION);
		Bundle.Entry entry = new Bundle.Entry();
		entry.setResource(appointment).setFullUrl("urn:uuid:1");
		bundle.addEntry(entry);

		FhirValidator validator = ourCtx.newValidator();
		validator.setValidateAgainstStandardSchema(true);
		validator.setValidateAgainstStandardSchematron(true);

		ValidationResult result = validator.validateWithResult(bundle);
		assertThat(result.isSuccessful()).as("Validation should have failed").isFalse();
		assertThat(result.getMessages()).hasSize(1);
		for (SingleValidationMessage singleValidationMessage : result.getMessages()) {
			ourLog.info(singleValidationMessage.getMessage());
		}
	}

	private Appointment createAppointment() {
		Appointment.Participant participant = new Appointment.Participant().setStatus(ParticipationStatusEnum.ACCEPTED).setType(ParticipantTypeEnum.PART);
		return new Appointment().setStatus(AppointmentStatusEnum.BOOKED).addParticipant(participant);
	}

	@AfterAll
	public static void afterClassClearContext() {
		TestUtil.randomizeLocaleAndTimezone();
	}
}
