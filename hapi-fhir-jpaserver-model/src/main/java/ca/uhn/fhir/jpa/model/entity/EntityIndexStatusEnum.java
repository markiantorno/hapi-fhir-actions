/*-
 * #%L
 * HAPI FHIR JPA Model
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
package ca.uhn.fhir.jpa.model.entity;

public enum EntityIndexStatusEnum {
	/**
	 * Only indexed in the relational database
	 */
	INDEXED_RDBMS_ONLY,

	/**
	 * Indexed in relational and fulltext databases
	 */
	INDEXED_ALL,

	/**
	 * Indexing failed - This should only happen if a resource is being reindexed and the reindexing fails
	 */
	INDEXING_FAILED;
}