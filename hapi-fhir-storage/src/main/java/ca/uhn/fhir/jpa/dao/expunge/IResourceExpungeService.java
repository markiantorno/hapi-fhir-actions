/*-
 * #%L
 * HAPI FHIR Storage api
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
package ca.uhn.fhir.jpa.dao.expunge;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.IResourcePersistentId;
import ca.uhn.fhir.rest.api.server.storage.IResourceVersionPersistentId;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <R> The resource PID type
 * @param <V> The resource version PID type
 */
public interface IResourceExpungeService<R extends IResourcePersistentId<?>, V extends IResourceVersionPersistentId> {
	List<R> findHistoricalVersionsOfDeletedResources(String theResourceName, R theResourceId, int theI);

	List<V> findHistoricalVersionsOfNonDeletedResources(String theResourceName, R theResourceId, int theI);

	void expungeHistoricalVersions(
			RequestDetails theRequestDetails, List<V> thePartition, AtomicInteger theRemainingCount);

	void expungeCurrentVersionOfResources(
			RequestDetails theRequestDetails, List<R> theResourceIds, AtomicInteger theRemainingCount);

	void expungeHistoricalVersionsOfIds(
			RequestDetails theRequestDetails, List<R> theResourceIds, AtomicInteger theRemainingCount);

	void deleteAllSearchParams(R theResourceId);
}
