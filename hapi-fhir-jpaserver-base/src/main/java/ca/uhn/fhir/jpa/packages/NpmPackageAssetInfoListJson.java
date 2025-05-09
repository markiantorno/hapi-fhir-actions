/*-
 * #%L
 * HAPI FHIR JPA Server
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
package ca.uhn.fhir.jpa.packages;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Represents details of an NPM resource and its associated package")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(
		creatorVisibility = JsonAutoDetect.Visibility.NONE,
		fieldVisibility = JsonAutoDetect.Visibility.NONE,
		getterVisibility = JsonAutoDetect.Visibility.NONE,
		isGetterVisibility = JsonAutoDetect.Visibility.NONE,
		setterVisibility = JsonAutoDetect.Visibility.NONE)
public class NpmPackageAssetInfoListJson {

	@JsonProperty("npmFhirIdPackageIdAndVersionJsons")
	private List<NpmPackageAssetInfoJson> myAssets;

	public NpmPackageAssetInfoListJson() {}

	public NpmPackageAssetInfoListJson(List<NpmPackageAssetInfoJson> theAssets) {
		myAssets = theAssets;
	}

	public List<NpmPackageAssetInfoJson> getAssets() {
		if (myAssets == null) {
			myAssets = new ArrayList<>();
		}
		return myAssets;
	}

	public void setAssets(List<NpmPackageAssetInfoJson> theAssets) {
		myAssets = theAssets;
	}
}
