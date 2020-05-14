/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.sample;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.InvestigationStatus;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.facility.FacilityReferenceDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.region.RegionReferenceDto;
import de.symeda.sormas.api.sample.*;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.api.visit.VisitDto;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.TestDataCreator.RDCFEntities;
import de.symeda.sormas.backend.facility.Facility;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class SampleFacadeEjbTest extends AbstractBeanTest {

	@Test
	public void testGetIndexList() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		SampleDto sample = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		SampleDto referredSample = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		sample.setReferredTo(referredSample.toReference());
		creator.createAdditionalTest(sample.toReference());
		creator.createAdditionalTest(sample.toReference());
		
		List<SampleIndexDto> sampleIndexDtos = getSampleFacade().getIndexList(new SampleCriteria(), 0, 100, null);
		
		// List should have one entry
		assertEquals(2, sampleIndexDtos.size());
		
		// First sample should have an additional test
		assertEquals(AdditionalTestingStatus.PERFORMED, sampleIndexDtos.get(1).getAdditionalTestingStatus());
	}

	@Test
	public void testGetIndexListBySampleSearchType() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person1");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);

		PersonDto contactPerson = creator.createPerson("Contact", "Person2");
		ContactDto contact = creator.createContact(user.toReference(), contactPerson.toReference(), caze);
		SampleDto cazeSample = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		SampleDto sample = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		SampleDto referredSample = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		sample.setReferredTo(referredSample.toReference());
		creator.createAdditionalTest(sample.toReference());

		assertEquals(3, getSampleFacade().count(new SampleCriteria()));

		final ArrayList<SortProperty> sortProperties = new ArrayList<>();
		sortProperties.add(new SortProperty(SampleDto.SAMPLE_DATE_TIME));
		final List<SampleIndexDto> sampleList1 = getSampleFacade().getIndexList(new SampleCriteria(), 0, 100,
				sortProperties);
		assertEquals(3, sampleList1.size());

		final SampleIndexDto sample11 = sampleList1.get(0);
		Assert.assertEquals(cazeSample.getUuid(), sample11.getUuid());
		Assert.assertEquals(caze.getUuid(), sample11.getAssociatedCase().getUuid());
		Assert.assertTrue(sample11.getAssociatedCase().getCaption().startsWith("Case PERSON1"));

		final SampleIndexDto sample12 = sampleList1.get(1);
		Assert.assertEquals(sample.getUuid(), sample12.getUuid());
		Assert.assertEquals(contact.getUuid(), sample12.getAssociatedContact().getUuid());
		Assert.assertEquals("Contact PERSON2", sample12.getAssociatedContact().getCaption());

		final SampleIndexDto sample13 = sampleList1.get(2);
		Assert.assertEquals(referredSample.getUuid(), sample13.getUuid());
		Assert.assertEquals(contact.getUuid(), sample13.getAssociatedContact().getUuid());
		Assert.assertEquals("Contact PERSON2", sample12.getAssociatedContact().getCaption());

		assertEquals(2, getSampleFacade().getIndexList(new SampleCriteria().sampleSearchType(SampleSearchType.CONTACT), 0, 100, null).size());
		assertEquals(1, getSampleFacade().getIndexList(new SampleCriteria().sampleSearchType(SampleSearchType.CASE), 0, 100, null).size());
	}

	@Test
	public void testGetIndexListForCaseConvertedFromContact() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person1");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);

		PersonDto contactPerson = creator.createPerson("Contact", "Person2");
		ContactDto contact = creator.createContact(user.toReference(), contactPerson.toReference(), caze);
		VisitDto visit = creator.createVisit(caze.getDisease(), contactPerson.toReference());
		SampleDto cazeSample = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		SampleDto sample = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		SampleDto referredSample = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		sample.setReferredTo(referredSample.toReference());
		creator.createAdditionalTest(sample.toReference());

		CaseDataDto caseDataDto = CaseDataDto.buildFromContact(contact, visit);
		caseDataDto.setRegion(new RegionReferenceDto(rdcf.region.getUuid()));
		caseDataDto.setDistrict(new DistrictReferenceDto(rdcf.district.getUuid()));
		caseDataDto.setHealthFacility(new FacilityReferenceDto(rdcf.facility.getUuid()));
		caseDataDto.setReportingUser(user.toReference());
		CaseDataDto caseConvertedFromContact = getCaseFacade().saveCase(caseDataDto);

		final SampleCriteria samplesConnectedToConvertedCaseCriteria = new SampleCriteria().caze(caseConvertedFromContact.toReference());
		assertEquals(2, getSampleFacade().count(samplesConnectedToConvertedCaseCriteria));

		final ArrayList<SortProperty> sortProperties = new ArrayList<>();
		sortProperties.add(new SortProperty(SampleDto.SAMPLE_DATE_TIME));
		final List<SampleIndexDto> samplesOfConvertedCase = getSampleFacade().getIndexList(samplesConnectedToConvertedCaseCriteria, 0, 100,
				sortProperties);
		assertEquals(2, samplesOfConvertedCase.size());

		final SampleIndexDto sample11 = samplesOfConvertedCase.get(0);
		Assert.assertEquals(sample.getUuid(), sample11.getUuid());
		Assert.assertEquals(caseConvertedFromContact.getUuid(), sample11.getAssociatedCase().getUuid());
		Assert.assertEquals(contact.getUuid(), sample11.getAssociatedContact().getUuid());

		final SampleIndexDto sample12 = samplesOfConvertedCase.get(1);
		Assert.assertEquals(referredSample.getUuid(), sample12.getUuid());
		Assert.assertEquals(contact.getUuid(), sample12.getAssociatedContact().getUuid());
		Assert.assertEquals(caseConvertedFromContact.getUuid(), sample11.getAssociatedCase().getUuid());
	}

	@Test
	public void testSampleDeletion() {
		Date since = new Date();
		
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		UserDto admin = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Ad", "Min", UserRole.ADMIN);
		String adminUuid = admin.getUuid();
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		SampleDto sample = creator.createSample(caze.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		PathogenTestDto sampleTest = creator.createPathogenTest(sample.toReference(), PathogenTestType.MICROSCOPY, caze.getDisease(), new Date(), rdcf.facility, user.toReference(), PathogenTestResultType.POSITIVE, "Positive", true);

		// Database should contain the created sample and sample test
		assertNotNull(getSampleFacade().getSampleByUuid(sample.getUuid()));
		assertNotNull(getSampleTestFacade().getByUuid(sampleTest.getUuid()));

		getSampleFacade().deleteSample(sample.toReference());

		// Sample and pathogen test should be marked as deleted
		assertTrue(getSampleFacade().getDeletedUuidsSince(since).contains(sample.getUuid()));
		assertTrue(getSampleTestFacade().getDeletedUuidsSince(since).contains(sampleTest.getUuid()));
	}
	
	@Test
	public void testArchivedSampleNotGettingTransfered() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		SampleDto sample = creator.createSample(caze.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		creator.createPathogenTest(sample.toReference(), PathogenTestType.MICROSCOPY, caze.getDisease(), new Date(), rdcf.facility, user.toReference(), PathogenTestResultType.POSITIVE, "Positive", true);

		// getAllActiveSamples/getAllActiveSampleTests and getAllUuids should return length 1
		assertEquals(1, getSampleFacade().getAllActiveSamplesAfter(null).size());
		assertEquals(1, getSampleFacade().getAllActiveUuids().size());
		assertEquals(1, getSampleTestFacade().getAllActivePathogenTestsAfter(null).size());
		assertEquals(1, getSampleTestFacade().getAllActiveUuids().size());
		
		getCaseFacade().archiveOrDearchiveCase(caze.getUuid(), true);
		
		// getAllActiveSamples/getAllActiveSampleTests and getAllUuids should return length 0
		assertEquals(0, getSampleFacade().getAllActiveSamplesAfter(null).size());
		assertEquals(0, getSampleFacade().getAllActiveUuids().size());
		assertEquals(0, getSampleTestFacade().getAllActivePathogenTestsAfter(null).size());
		assertEquals(0, getSampleTestFacade().getAllActiveUuids().size());

		getCaseFacade().archiveOrDearchiveCase(caze.getUuid(), false);

		// getAllActiveSamples/getAllActiveSampleTests and getAllUuids should return length 1
		assertEquals(1, getSampleFacade().getAllActiveSamplesAfter(null).size());
		assertEquals(1, getSampleFacade().getAllActiveUuids().size());
		assertEquals(1, getSampleTestFacade().getAllActivePathogenTestsAfter(null).size());
		assertEquals(1, getSampleTestFacade().getAllActiveUuids().size());
	}

	@Test
	public void testGetNewTestResultCountByResultType() {
		RDCFEntities rdcf = creator.createRDCFEntities();
		UserReferenceDto user = creator.createUser(rdcf).toReference();
		PersonReferenceDto person1 = creator.createPerson("Heinz", "First").toReference();
		PersonReferenceDto person2 = creator.createPerson("Heinz", "Second").toReference();
		CaseDataDto case1 = creator.createCase(user, person1, rdcf);
		CaseDataDto case2 = creator.createCase(user, person2, rdcf);
		
		List<Long> caseIds = getCaseService().getAllIds(null);

		// no existing samples
		SampleFacade sampleFacade = getSampleFacade();
		Map<PathogenTestResultType, Long> resultMap = sampleFacade.getNewTestResultCountByResultType(caseIds);
		assertEquals(new Long(0), resultMap.values().stream().collect(Collectors.summingLong(Long::longValue)));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.INDETERMINATE, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.NEGATIVE, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.PENDING, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.POSITIVE, null));

		// one pending sample with in one case
		Facility lab = creator.createFacility("facility", rdcf.region, rdcf.district, rdcf.community);
		creator.createSample(case1.toReference(), user, lab);

		resultMap = sampleFacade.getNewTestResultCountByResultType(caseIds);
		assertEquals(new Long(1), resultMap.values().stream().collect(Collectors.summingLong(Long::longValue)));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.INDETERMINATE, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.NEGATIVE, null));
		assertEquals(new Long(1), resultMap.getOrDefault(PathogenTestResultType.PENDING, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.POSITIVE, null));

		// one pending sample in each of two cases
		creator.createSample(case2.toReference(), user, lab);

		resultMap = sampleFacade.getNewTestResultCountByResultType(caseIds);
		assertEquals(new Long(2), resultMap.values().stream().collect(Collectors.summingLong(Long::longValue)));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.INDETERMINATE, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.NEGATIVE, null));
		assertEquals(new Long(2), resultMap.getOrDefault(PathogenTestResultType.PENDING, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.POSITIVE, null));

		// one pending sample in each of two cases
		// and one positive sample in one of the two cases
		SampleDto sample = creator.createSample(case1.toReference(), user, lab);
		sample.setPathogenTestResult(PathogenTestResultType.POSITIVE);
		sampleFacade.saveSample(sample);

		resultMap = sampleFacade.getNewTestResultCountByResultType(caseIds);
		assertEquals(new Long(2), resultMap.values().stream().collect(Collectors.summingLong(Long::longValue)));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.INDETERMINATE, null));
		assertNull(resultMap.getOrDefault(PathogenTestResultType.NEGATIVE, null));
		assertEquals(new Long(1), resultMap.getOrDefault(PathogenTestResultType.PENDING, null));
		assertEquals(new Long(1), resultMap.getOrDefault(PathogenTestResultType.POSITIVE, null));
	}
	

	@Test
	public void testGetByCaseUuids() throws Exception {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf, UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto person = creator.createPerson();
		CaseDataDto caze = creator.createCase(user.toReference(), person.toReference(), rdcf);
		CaseDataDto caze2 = creator.createCase(user.toReference(), person.toReference(), rdcf);
		CaseDataDto caze3 = creator.createCase(user.toReference(), person.toReference(), rdcf);
		
		SampleDto sample = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		SampleDto sample2 = creator.createSample(caze.toReference(), user.toReference(), rdcf.facility);
		SampleDto sample3 = creator.createSample(caze2.toReference(), user.toReference(), rdcf.facility);
		creator.createSample(caze3.toReference(), user.toReference(), rdcf.facility);
		
		List<SampleDto> samples = getSampleFacade().getByCaseUuids(Arrays.asList(caze.getUuid(), caze2.getUuid()));
		
		assertThat(samples, hasSize(3));
		assertThat(samples, contains(sample, sample2, sample3));
	}

}
