/*-
 * #%L
 * HAPI FHIR - Clinical Reasoning
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.cr.dstu3.questionnaire;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.springframework.beans.factory.annotation.Autowired;

import static ca.uhn.fhir.cr.common.CanonicalHelper.getCanonicalType;
import static ca.uhn.fhir.cr.common.IdHelper.getIdType;

public class QuestionnaireDataRequirementsProvider {
	@Autowired
	IQuestionnaireProcessorFactory myQuestionnaireFactory;

	@Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = Questionnaire.class)
	public IBaseResource getDataRequirements(
			@IdParam IdType theId,
			@OperationParam(name = "canonical") String theCanonical,
			@OperationParam(name = "url") String theUrl,
			@OperationParam(name = "version") String theVersion,
			RequestDetails theRequestDetails)
			throws InternalErrorException, FHIRException {
		StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, theCanonical, theUrl, theVersion);
		return myQuestionnaireFactory
				.create(theRequestDetails)
				.dataRequirements(Eithers.for3(canonicalType, theId, null), null);
	}

	@Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = Questionnaire.class)
	public IBaseResource getDataRequirements(
			@OperationParam(name = "id") String theId,
			@OperationParam(name = "canonical") String theCanonical,
			@OperationParam(name = "url") String theUrl,
			@OperationParam(name = "version") String theVersion,
			RequestDetails theRequestDetails)
			throws InternalErrorException, FHIRException {
		IIdType id = getIdType(FhirVersionEnum.DSTU3, "Questionnaire", theId);
		StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, theCanonical, theUrl, theVersion);
		return myQuestionnaireFactory
				.create(theRequestDetails)
				.dataRequirements(Eithers.for3(canonicalType, id, null), null);
	}
}