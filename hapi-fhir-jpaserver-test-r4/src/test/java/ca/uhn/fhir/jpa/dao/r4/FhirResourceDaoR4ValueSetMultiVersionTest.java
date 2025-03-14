package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.data.ITermValueSetConceptDao;
import ca.uhn.fhir.jpa.entity.TermValueSet;
import ca.uhn.fhir.jpa.model.dao.JpaPid;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;
import ca.uhn.fhir.jpa.util.ValueSetTestUtil;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class FhirResourceDaoR4ValueSetMultiVersionTest extends BaseJpaR4Test {

	public static final String URL_MY_VALUE_SET = "http://example.com/my_value_set";
	public static final String URL_MY_CODE_SYSTEM = "http://example.com/my_code_system";
	@Autowired
	protected ITermValueSetConceptDao myTermValueSetConceptDao;
	private final ValueSetTestUtil myValueSetTestUtil = new ValueSetTestUtil(FhirVersionEnum.R4);

	private DaoMethodOutcome createLocalCsAndVs(String theVersion, Set<String> theCodeSystemCodes) {
		CodeSystem codeSystem = new CodeSystem();
		codeSystem.setUrl(URL_MY_CODE_SYSTEM);
		codeSystem.setVersion(theVersion);
		codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
		for (String codeSystemCode : theCodeSystemCodes) {
			codeSystem.addConcept().setCode(codeSystemCode);
		}
		myCodeSystemDao.create(codeSystem, mySrd);

		return createLocalVs(codeSystem, theVersion);

	}

	private DaoMethodOutcome createLocalVs(CodeSystem theCodeSystem, String theVersion) {
		ValueSet valueSet = new ValueSet();
		valueSet.setUrl(URL_MY_VALUE_SET);
		valueSet.setVersion(theVersion);
		if (theVersion == null) {
			valueSet.setName("ValueSet_noVersion");
		} else {
			valueSet.setName("ValueSet_" + theVersion);
		}
		valueSet.getCompose().addInclude().setSystem(theCodeSystem.getUrl());
		return myValueSetDao.create(valueSet, mySrd);
	}

	private Map<ValueSetVersions, DaoMethodOutcome> createVersionedValueSets() {

		HashMap<ValueSetVersions, DaoMethodOutcome> valueSets = new HashMap<>();

		Set<String> valueSetConcepts_noVersion = new HashSet<>();
		valueSetConcepts_noVersion.add("hello");
		valueSetConcepts_noVersion.add("goodbye");
		valueSets.put(ValueSetVersions.NULL, createLocalCsAndVs(null, valueSetConcepts_noVersion));

		Set<String> valueSetConcepts_v1 = new HashSet<>(valueSetConcepts_noVersion);
		valueSetConcepts_v1.add("hi");
		valueSets.put(ValueSetVersions.V1, createLocalCsAndVs("v1", valueSetConcepts_v1));

		Set<String> valueSetConcepts_v2 = new HashSet<>(valueSetConcepts_v1);
		valueSetConcepts_v2.add("so-long");
		valueSets.put(ValueSetVersions.V2, createLocalCsAndVs("v2", valueSetConcepts_v2));

		return valueSets;
	}

	@Test
	public void testCreateVersionedValueSets() {
		Map<ValueSetVersions, DaoMethodOutcome> myValueSets = createVersionedValueSets();

		runInTransaction(() -> {
			assertEquals(3, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());

			Optional<TermValueSet> optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET);
			assertTrue(optionalTermValueSet.isPresent());
			JpaPid nullVersion_resid = ((ResourceTable) myValueSets.get(ValueSetVersions.NULL).getEntity()).getId();
			assertNotNull(nullVersion_resid);
			assertNotNull(optionalTermValueSet.get().getResource());
			assertEquals(nullVersion_resid, optionalTermValueSet.get().getResource().getId());
			assertEquals("ValueSet_noVersion", optionalTermValueSet.get().getName());

			optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1");
			assertTrue(optionalTermValueSet.isPresent());
			JpaPid v1Version_resid = ((ResourceTable) myValueSets.get(ValueSetVersions.V1).getEntity()).getId();
			assertNotNull(v1Version_resid);
			assertNotNull(optionalTermValueSet.get().getResource());
			assertEquals(v1Version_resid, optionalTermValueSet.get().getResource().getId());
			assertEquals("ValueSet_v1", optionalTermValueSet.get().getName());

			optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2");
			assertTrue(optionalTermValueSet.isPresent());
			JpaPid v2Version_resid = ((ResourceTable) myValueSets.get(ValueSetVersions.V2).getEntity()).getId();
			assertNotNull(v2Version_resid);
			assertNotNull(optionalTermValueSet.get().getResource());
			assertEquals(v2Version_resid, optionalTermValueSet.get().getResource().getId());
			assertEquals("ValueSet_v2", optionalTermValueSet.get().getName());
		});

	}

	@Test
	public void testUpdateVersionedValueSets() {
		Map<ValueSetVersions, DaoMethodOutcome> myValueSets = createVersionedValueSets();

		runInTransaction(() -> {
			assertEquals(3, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());

			TermValueSet vs = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET).orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " with null version"));
			assertEquals("ValueSet_noVersion", vs.getName());

			vs = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v1"));
			assertEquals("ValueSet_v1", vs.getName());

			vs = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v2"));
			assertEquals("ValueSet_v2", vs.getName());
		});

		// Update ValueSets
		ValueSet updated = (ValueSet) myValueSets.get(ValueSetVersions.NULL).getResource();
		updated.setName("ValueSet_noVersion_updated");
		DaoMethodOutcome nullVersion_update_outcome = myValueSetDao.update(updated);
		JpaPid nullVersion_resid = ((ResourceTable) nullVersion_update_outcome.getEntity()).getId();

		updated = (ValueSet) myValueSets.get(ValueSetVersions.V1).getResource();
		updated.setName("ValueSet_v1_updated");
		DaoMethodOutcome v1Version_update_outcome = myValueSetDao.update(updated);
		JpaPid v1Version_resid = ((ResourceTable) v1Version_update_outcome.getEntity()).getId();

		updated = (ValueSet) myValueSets.get(ValueSetVersions.V2).getResource();
		updated.setName("ValueSet_v2_updated");
		DaoMethodOutcome v2Version_update_outcome = myValueSetDao.update(updated);
		JpaPid v2Version_resid = ((ResourceTable) v2Version_update_outcome.getEntity()).getId();

		// Verify that ValueSets were updated.
		runInTransaction(() -> assertEquals(3, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size()));

		runInTransaction(() -> {
			TermValueSet termValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET).orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " with null version"));
			assertNotNull(nullVersion_resid);
			assertNotNull(termValueSet.getResource());
			assertEquals(nullVersion_resid, termValueSet.getResource().getId());
			assertEquals("ValueSet_noVersion_updated", termValueSet.getName());

			termValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v1"));
			assertNotNull(v1Version_resid);
			assertNotNull(termValueSet.getResource());
			assertEquals(v1Version_resid, termValueSet.getResource().getId());
			assertEquals("ValueSet_v1_updated", termValueSet.getName());

			termValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v2"));
			assertNotNull(v2Version_resid);
			assertNotNull(termValueSet.getResource());
			assertEquals(v2Version_resid, termValueSet.getResource().getId());
			assertEquals("ValueSet_v2_updated", termValueSet.getName());
		});
	}

	@Test
	public void testDeleteVersionedValueSets() {
		Map<ValueSetVersions, DaoMethodOutcome> myValueSets = createVersionedValueSets();

		runInTransaction(() -> {
			assertEquals(3, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());

			TermValueSet termValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET).orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " with null version"));
			assertEquals("ValueSet_noVersion", termValueSet.getName());

			termValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v1"));
			assertEquals("ValueSet_v1", termValueSet.getName());

			termValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v2"));
			assertEquals("ValueSet_v2", termValueSet.getName());
		});

		// Delete ValueSets
		myValueSetDao.delete(myValueSets.get(ValueSetVersions.NULL).getResource().getIdElement());
		runInTransaction(() -> {
			assertEquals(2, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());
			Optional<TermValueSet> optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET);
			if (optionalTermValueSet.isPresent()) {
				fail();			}
			assertNotNull(myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v1")));
			assertNotNull(myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v2")));
		});

		myValueSetDao.delete(myValueSets.get(ValueSetVersions.V1).getResource().getIdElement());

		runInTransaction(() -> {
			assertEquals(1, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());
			Optional<TermValueSet> optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET);
			if (optionalTermValueSet.isPresent()) {
				fail();			}
			optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1");
			if (optionalTermValueSet.isPresent()) {
				fail();			}
			assertNotNull(myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2").orElseThrow(() -> new IllegalArgumentException("No TerValueSet found for " + URL_MY_VALUE_SET + " version v2")));
		});

		myValueSetDao.delete(myValueSets.get(ValueSetVersions.V2).getResource().getIdElement());
		runInTransaction(() -> {
			assertEquals(0, myTermValueSetDao.findTermValueSetByUrl(PageRequest.of(0, 10), URL_MY_VALUE_SET).size());
			Optional<TermValueSet> optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndNullVersion(URL_MY_VALUE_SET);
			if (optionalTermValueSet.isPresent()) {
				fail();			}
			optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v1");
			if (optionalTermValueSet.isPresent()) {
				fail();			}
			optionalTermValueSet = myTermValueSetDao.findTermValueSetByUrlAndVersion(URL_MY_VALUE_SET, "v2");
			if (optionalTermValueSet.isPresent()) {
				fail();			}
		});
	}

	@Test
	public void testExpandWithNoResultsInLocalValueSet() {
		Set<String> valueSetConcepts = new HashSet<>();
		valueSetConcepts.add("hello");
		valueSetConcepts.add("goodbye");

		createLocalCsAndVs("1", valueSetConcepts);

		ValueSet vs = new ValueSet();
		ValueSet.ConceptSetComponent include = vs.getCompose().addInclude();
		include.setSystem(URL_MY_CODE_SYSTEM + "AA");
		include.setVersion("1");
		include.addConcept().setCode("A");

		ValueSet expansion = myValueSetDao.expand(vs, null);
		assertThat(myValueSetTestUtil.toCodes(expansion)).containsExactly("A");

	}

	private enum ValueSetVersions {NULL, V1, V2}


}
