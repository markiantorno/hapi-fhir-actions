/*-
 * #%L
 * HAPI FHIR - Core Library
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
package ca.uhn.fhir.util.adapters;

import jakarta.annotation.Nonnull;

import java.util.Optional;

/**
 * Generic version of Eclipse IAdaptable interface.
 */
public interface IAdaptable {
	/**
	 * Get an adapter of requested type.
	 * @param theTargetType the desired type of the adapter
	 * @return an adapter of theTargetType if possible, or empty.
	 */
	default <T> @Nonnull Optional<T> getAdapter(@Nonnull Class<T> theTargetType) {
		return AdapterUtils.adapt(this, theTargetType);
	}
}