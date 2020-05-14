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
package de.symeda.sormas.backend.contact;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.contact.*;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.sample.SampleMaterial;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.caze.InvestigationStatus;
import de.symeda.sormas.api.caze.MapCaseDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.region.RegionReferenceDto;
import de.symeda.sormas.api.symptoms.SymptomState;
import de.symeda.sormas.api.task.TaskContext;
import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.task.TaskStatus;
import de.symeda.sormas.api.task.TaskType;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.visit.VisitDto;
import de.symeda.sormas.api.visit.VisitStatus;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.MockProducer;
import de.symeda.sormas.backend.TestDataCreator.RDCFEntities;
import de.symeda.sormas.backend.util.DateHelper8;
import de.symeda.sormas.backend.visit.Visit;

public class ContactFacadeEjbTest extends AbstractBeanTest  {

	@Test
	public void testUpdateFollowUpUntilAndStatus() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid()
				,"Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact = creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);

		assertEquals(FollowUpStatus.FOLLOW_UP, contact.getFollowUpStatus());
		assertEquals(LocalDate.now().plusDays(21), DateHelper8.toLocalDate(contact.getFollowUpUntil()));

		VisitDto visit = creator.createVisit(caze.getDisease(), contactPerson.toReference(), DateUtils.addDays(new Date(), 21), VisitStatus.UNAVAILABLE);

		// should now be one day more
		contact = getContactFacade().getContactByUuid(contact.getUuid());
		assertEquals(FollowUpStatus.FOLLOW_UP, contact.getFollowUpStatus());
		assertEquals(LocalDate.now().plusDays(21+1), DateHelper8.toLocalDate(contact.getFollowUpUntil()));

		visit.setVisitStatus(VisitStatus.COOPERATIVE);
		visit = getVisitFacade().saveVisit(visit);

		// and now the old date again - and done
		contact =  getContactFacade().getContactByUuid(contact.getUuid());
		assertEquals(FollowUpStatus.COMPLETED, contact.getFollowUpStatus());
		assertEquals(LocalDate.now().plusDays(21), DateHelper8.toLocalDate(contact.getFollowUpUntil()));
	}

	@Test
	public void testGetMatchingContacts(){
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid()
				,"Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.CORONAVIRUS, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact1 = creator.createContact(user.toReference(), user.toReference(),
				contactPerson.toReference(), caze, new Date(), new Date(), null);
		contact1.setContactClassification(ContactClassification.CONFIRMED);
		getContactFacade().saveContact(contact1);
		ContactDto contact2 = creator.createContact(user.toReference(), user.toReference(),
				contactPerson.toReference(), caze, DateHelper.subtractDays(new Date(), 15), new Date(), null);
		ContactDto contact3 = creator.createContact(user.toReference(), user.toReference(),
				contactPerson.toReference(), caze, DateHelper.subtractDays(new Date(), 15),
				DateHelper.subtractDays(new Date(), 31), null);

		final ContactSimilarityCriteria contactSimilarityCriteria = new ContactSimilarityCriteria();
		contactSimilarityCriteria.setDisease(Disease.CORONAVIRUS);
		contactSimilarityCriteria.setPerson(new PersonReferenceDto(contactPerson.getUuid()));
		contactSimilarityCriteria.setCaze(new CaseReferenceDto(caze.getUuid()));
		contactSimilarityCriteria.setLastContactDate(new Date());
		contactSimilarityCriteria.setReportDate(new Date());

		final List<SimilarContactDto> matchingContacts = getContactFacade().getMatchingContacts(contactSimilarityCriteria);
		Assert.assertNotNull(matchingContacts);
		Assert.assertEquals(2, matchingContacts.size());
		final SimilarContactDto similarContactDto1 = matchingContacts.get(0);
		Assert.assertEquals(contact1.getUuid(), similarContactDto1.getUuid());
		final SimilarContactDto similarContactDto2 = matchingContacts.get(1);
		Assert.assertEquals(contact2.getUuid(), similarContactDto2.getUuid());
	}

	@Test
	public void testUpdateContactStatus() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid()
				,"Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		Date contactDate = new Date();
		ContactDto contact = creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, contactDate, contactDate, null);

		assertEquals(ContactStatus.ACTIVE, contact.getContactStatus());
		assertNull(contact.getResultingCase());

		// drop
		contact.setContactClassification(ContactClassification.NO_CONTACT);
		contact = getContactFacade().saveContact(contact);
		assertEquals(ContactStatus.DROPPED, contact.getContactStatus());

		// add result case
		CaseDataDto resultingCaze = creator.createCase(user.toReference(), contactPerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, contactDate, rdcf);
		contact.setContactClassification(ContactClassification.CONFIRMED);
		contact.setResultingCase(getCaseFacade().getReferenceByUuid(resultingCaze.getUuid()));
		contact = getContactFacade().saveContact(contact);
		assertEquals(ContactStatus.CONVERTED, contact.getContactStatus());
	}

	@Test
	public void testGenerateContactFollowUpTasks() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		UserDto contactOfficer = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Cont", "Off", UserRole.CONTACT_OFFICER);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact = creator.createContact(user.toReference(), contactOfficer.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);

		getContactFacade().generateContactFollowUpTasks();

		// task should have been generated
		List<TaskDto> tasks = getTaskFacade().getAllByContact(contact.toReference());
		assertEquals(1, tasks.size());
		TaskDto task = tasks.get(0);
		assertEquals(TaskType.CONTACT_FOLLOW_UP, task.getTaskType());
		assertEquals(TaskStatus.PENDING, task.getTaskStatus());
		assertEquals(LocalDate.now(), DateHelper8.toLocalDate(task.getDueDate()));
		assertEquals(contactOfficer.toReference(), task.getAssigneeUser());

		// task should not be generated multiple times 
		 getContactFacade().generateContactFollowUpTasks();
		tasks = getTaskFacade().getAllByContact(contact.toReference());
		assertEquals(1, tasks.size());
	}

	@Test
	public void testMapContactListCreation() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);
		MapCaseDto mapCaseDto = new MapCaseDto(caze.getUuid(), caze.getReportDate(), caze.getCaseClassification(), caze.getDisease(),
				caze.getPerson().getUuid(), cazePerson.getFirstName(), cazePerson.getLastName(),
				caze.getHealthFacility().getUuid(), 0d, 0d,
				caze.getReportLat(), caze.getReportLon(), caze.getReportLat(), caze.getReportLon());

		List<MapContactDto> mapContactDtos =  getContactFacade().getContactsForMap(caze.getRegion(), caze.getDistrict(), caze.getDisease(), DateHelper.subtractDays(new Date(),  1), DateHelper.addDays(new Date(), 1), Arrays.asList(mapCaseDto));

		// List should have one entry
		assertEquals(1, mapContactDtos.size());
	}

	@Test
	public void testContactDeletion() {
		Date since = new Date();

		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		UserDto admin = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Ad", "Min", UserRole.ADMIN);
		String adminUuid = admin.getUuid();
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact = creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);
		VisitDto visit = creator.createVisit(caze.getDisease(), contactPerson.toReference(), DateUtils.addDays(new Date(), 21), VisitStatus.UNAVAILABLE);
		TaskDto task = creator.createTask(TaskContext.CONTACT, TaskType.CONTACT_INVESTIGATION, TaskStatus.PENDING, null, contact.toReference(), null, new Date(), user.toReference());
		SampleDto sample = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		SampleDto sample2 = creator.createSample(contact.toReference(), new Date(), new Date(), user.toReference(), SampleMaterial.BLOOD, rdcf.facility);
		sample2.setAssociatedCase(new CaseReferenceDto(caze.getUuid()));
		getSampleFacade().saveSample(sample2);

		// Database should contain the created contact, visit and task
		assertNotNull(getContactFacade().getContactByUuid(contact.getUuid()));
		assertNotNull(getTaskFacade().getByUuid(task.getUuid()));
		assertNotNull(getVisitFacade().getVisitByUuid(visit.getUuid()));
		assertNotNull(getSampleFacade().getSampleByUuid(sample.getUuid()));

		getContactFacade().deleteContact(contact.getUuid());

		// Deleted flag should be set for contact; Task should be deleted
		assertTrue(getContactFacade().getDeletedUuidsSince(since).contains(contact.getUuid()));
		// Can't delete visit because it might be associated with other contacts as well
//		assertNull(getVisitFacade().getVisitByUuid(visit.getUuid()));
		assertNull(getTaskFacade().getByUuid(task.getUuid()));
		assertTrue(getSampleFacade().getDeletedUuidsSince(since).contains(sample.getUuid()));
		assertFalse(getSampleFacade().getDeletedUuidsSince(since).contains(sample2.getUuid()));
	}

	@Test
	public void testGetIndexList() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);

		// Database should contain one contact, associated visit and task
		assertEquals(1, getContactFacade().getIndexList(null, 0, 100, null).size());
	}

	@Test
	public void testGetContactCountsByCasesForDashboard() {

		List<String> uuids;

		// test with some random uuid: returns 0,0,0
		uuids = Arrays.asList("some-uuid");
		int[] result = getContactFacade().getContactCountsByCasesForDashboard(uuids);
		assertThat(result[0], equalTo(0));
		assertThat(result[1], equalTo(0));
		assertThat(result[2], equalTo(0));
	}

	@Test
	public void testCreatedContactExistWhenValidatedByUUID() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact = creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);

		// database contains the created contact
		assertEquals(true, getContactFacade().isValidContactUuid(contact.getUuid()));
		// database contains the created contact
		assertEquals(false, getContactFacade().isValidContactUuid("nonExistingContactUUID"));
	}

	@Test
	public void testGetExportList() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		String userUuid = user.getUuid();
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);
		VisitDto visit = creator.createVisit(caze.getDisease(), contactPerson.toReference(), new Date(), VisitStatus.COOPERATIVE);

		contactPerson.getAddress().setRegion(new RegionReferenceDto(rdcf.region.getUuid()));
		contactPerson.getAddress().setDistrict(new DistrictReferenceDto(rdcf.district.getUuid()));
		contactPerson.getAddress().setCity("City");
		contactPerson.getAddress().setAddress("Street Address");
		contactPerson.getAddress().setPostalCode("1234");
		getPersonFacade().savePerson(contactPerson);

		visit.getSymptoms().setAbdominalPain(SymptomState.YES);
		getVisitFacade().saveVisit(visit);

		List<ContactExportDto> results = getContactFacade().getExportList(null, 0, 100, Language.EN);

		// Database should contain one contact, associated visit and task
		assertEquals(1, results.size());

		// Make sure that everything that is added retrospectively (address, last cooperative visit date and symptoms) is present
		ContactExportDto exportDto = results.get(0);

		assertEquals(rdcf.region.getName(), exportDto.getAddressRegion());
		assertEquals(rdcf.district.getName(), exportDto.getAddressDistrict());
		assertEquals("City", exportDto.getCity());
		assertEquals("Street Address", exportDto.getAddress());
		assertEquals("1234", exportDto.getPostalCode());

		assertNotNull(exportDto.getLastCooperativeVisitDate());
		assertTrue(StringUtils.isNotEmpty(exportDto.getLastCooperativeVisitSymptoms()));
		assertEquals(exportDto.getLastCooperativeVisitSymptomatic(), YesNoUnknown.YES);
	}

	@Test
	public void testGetContactVisitsExportList() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		String userUuid = user.getUuid();
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		ContactDto contact = creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference()
				, caze, new Date(), new Date(), null);
		VisitDto visit1 = creator.createVisit(caze.getDisease(), contactPerson.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit1.getSymptoms().setAbdominalPain(SymptomState.YES);
		getVisitFacade().saveVisit(visit1);
		VisitDto visit12 = creator.createVisit(caze.getDisease(), contactPerson.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit12.getSymptoms().setChestPain(SymptomState.YES);
		getVisitFacade().saveVisit(visit12);

		PersonDto contactPerson2 = creator.createPerson("Contact2", "Person2");
		ContactDto contact2 = creator.createContact(user.toReference(), user.toReference(), contactPerson2.toReference()
				, caze, new Date(), null, null);
		VisitDto visit21 = creator.createVisit(caze.getDisease(), contactPerson2.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit21.getSymptoms().setBackache(SymptomState.YES);
		getVisitFacade().saveVisit(visit21);

		final List<ContactVisitsExportDto> results = getContactFacade().getContactVisitsExportList(null, 0, 100, Language.EN);
		assertNotNull(results);
		assertEquals(2, results.size());

		final ContactVisitsExportDto exportDto1 = results.get(0);
		assertEquals("Contact", exportDto1.getFirstName());
		assertEquals("Person", exportDto1.getLastName());
		assertEquals(contact.getUuid(), exportDto1.getUuid());
		final List<ContactVisitsExportDto.ContactVisitsDetailsExportDto> visitDetails = exportDto1.getVisitDetails();
		assertNotNull(visitDetails);
		assertEquals(2, visitDetails.size());
		final ContactVisitsExportDto.ContactVisitsDetailsExportDto visitDetail11 = visitDetails.get(0);
		assertEquals(VisitStatus.COOPERATIVE, visitDetail11.getVisitStatus());
		assertNotNull(visitDetail11.getVisitDateTime());
		assertEquals("Abdominal pain", visitDetail11.getSymptoms());
		final ContactVisitsExportDto.ContactVisitsDetailsExportDto visitDetail12 = visitDetails.get(1);
		assertEquals(VisitStatus.COOPERATIVE, visitDetail12.getVisitStatus());
		assertNotNull(visitDetail12.getVisitDateTime());
		assertEquals("Chest pain", visitDetail12.getSymptoms());

		final ContactVisitsExportDto exportDto2 = results.get(1);
		assertEquals("Contact2", exportDto2.getFirstName());
		assertEquals("Person2", exportDto2.getLastName());
		assertEquals(contact2.getUuid(), exportDto2.getUuid());
		final List<ContactVisitsExportDto.ContactVisitsDetailsExportDto> visitDetails2 = exportDto2.getVisitDetails();
		assertNotNull(visitDetails2);
		assertEquals(1, visitDetails2.size());
		final ContactVisitsExportDto.ContactVisitsDetailsExportDto visitDetail21 = visitDetails2.get(0);
		assertEquals(VisitStatus.COOPERATIVE, visitDetail21.getVisitStatus());
		assertNotNull(visitDetail21.getVisitDateTime());
		assertEquals("Backache", visitDetail21.getSymptoms());
	}

	@Test
	public void testCountMaximumFollowUps() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(), "Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD, CaseClassification.PROBABLE,
				InvestigationStatus.PENDING, new Date(), rdcf);

		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference()
				, caze, new Date(), new Date(), null);
		VisitDto visit = creator.createVisit(caze.getDisease(), contactPerson.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit.getSymptoms().setAbdominalPain(SymptomState.YES);
		getVisitFacade().saveVisit(visit);

		PersonDto contactPerson2 = creator.createPerson("Contact2", "Person2");
		creator.createContact(user.toReference(), user.toReference(), contactPerson2.toReference()
				, caze, new Date(), new Date(), null);
		VisitDto visit21 = creator.createVisit(caze.getDisease(), contactPerson2.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit21.getSymptoms().setAbdominalPain(SymptomState.YES);
		getVisitFacade().saveVisit(visit21);
		VisitDto visit22 = creator.createVisit(caze.getDisease(), contactPerson2.toReference(), new Date(), VisitStatus.COOPERATIVE);
		visit22.getSymptoms().setAgitation(SymptomState.YES);
		getVisitFacade().saveVisit(visit22);

		PersonDto contactPerson3 = creator.createPerson("Contact3", "Person3");
		creator.createContact(user.toReference(), user.toReference(), contactPerson3.toReference()
				, caze, new Date(), new Date(), null);
		for (int i=0;i<10;i++){
			creator.createVisit(caze.getDisease(), contactPerson3.toReference(), new Date(), VisitStatus.COOPERATIVE);
		}

		assertEquals(10, getContactFacade().countMaximumFollowUps(null));
	}

	@Test
	public void testArchiveOrDearchiveContact() {
		RDCFEntities rdcf = creator.createRDCFEntities("Region", "District", "Community", "Facility");
		UserDto user = creator.createUser(rdcf.region.getUuid(), rdcf.district.getUuid(), rdcf.facility.getUuid(),
				"Surv", "Sup", UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto cazePerson = creator.createPerson("Case", "Person");
		CaseDataDto caze = creator.createCase(user.toReference(), cazePerson.toReference(), Disease.EVD,
				CaseClassification.PROBABLE, InvestigationStatus.PENDING, new Date(), rdcf);
		PersonDto contactPerson = creator.createPerson("Contact", "Person");
		creator.createContact(user.toReference(), user.toReference(), contactPerson.toReference(), caze, new Date(), new Date(), null);
		creator.createVisit(caze.getDisease(), contactPerson.toReference(), new Date(), VisitStatus.COOPERATIVE);

		when(MockProducer.getPrincipal().getName()).thenReturn("SurvSup");

		// getAllActiveContacts and getAllUuids should return length 1
		assertEquals(1, getContactFacade().getAllActiveContactsAfter(null).size());
		assertEquals(1, getContactFacade().getAllActiveUuids().size());
		assertEquals(1, getVisitFacade().getAllActiveVisitsAfter(null).size());
		assertEquals(1, getVisitFacade().getAllActiveUuids().size());

		getCaseFacade().archiveOrDearchiveCase(caze.getUuid(), true);

		// getAllActiveContacts and getAllUuids should return length 0
		assertEquals(0, getContactFacade().getAllActiveContactsAfter(null).size());
		assertEquals(0, getContactFacade().getAllActiveUuids().size());
		assertEquals(0, getVisitFacade().getAllActiveVisitsAfter(null).size());
		assertEquals(0, getVisitFacade().getAllActiveUuids().size());

		getCaseFacade().archiveOrDearchiveCase(caze.getUuid(), false);

		// getAllActiveContacts and getAllUuids should return length 1
		assertEquals(1, getContactFacade().getAllActiveContactsAfter(null).size());
		assertEquals(1, getContactFacade().getAllActiveUuids().size());
		assertEquals(1, getVisitFacade().getAllActiveVisitsAfter(null).size());
		assertEquals(1, getVisitFacade().getAllActiveUuids().size());
	}

	@Test
	public void testUpdateContactVisitAssociations() throws Exception {
		UserDto user = creator.createUser(creator.createRDCFEntities(), UserRole.SURVEILLANCE_SUPERVISOR);
		PersonDto person = creator.createPerson();
		VisitDto visit = creator.createVisit(Disease.EVD, person.toReference());
		ContactDto contact = creator.createContact(user.toReference(), person.toReference());
		Contact contactEntity = getContactService().getByUuid(contact.getUuid());
		Visit visitEntity = getVisitService().getByUuid(visit.getUuid());
		
		// Saved contact should have visit association
		assertThat(getVisitService().getAllByContact(contactEntity), hasSize(1));
		
		// Updating the contact but not changing the report date or last contact date should not alter the association
		contact.setDescription("Description");
		getContactFacade().saveContact(contact);
		
		assertThat(getVisitService().getAllByContact(contactEntity), hasSize(1));
		
		// Changing the report date to a value beyond the threshold should remove the association
		contact.setReportDateTime(DateHelper.addDays(visit.getVisitDateTime(), ContactLogic.ALLOWED_CONTACT_DATE_OFFSET + 20));
		getContactFacade().saveContact(contact);
		
		assertThat(getVisitService().getAllByContact(contactEntity), empty());
		
		// Changing the report date back to a value in the threshold should re-add the association
		contact.setReportDateTime(new Date());
		getContactFacade().saveContact(contact);
		
		assertThat(getVisitService().getAllByContact(contactEntity), hasSize(1));
		
		// Adding another contact that matches the visit person, disease and time frame should increase the collection size
		ContactDto contact2 = creator.createContact(user.toReference(), person.toReference());
		
		assertThat(getContactService().getAllByVisit(visitEntity), hasSize(2));
		
		// Adding another contact with the same person and disease, but an incompatible time frame should not increase the collection size
		creator.createContact(user.toReference(), person.toReference(), DateHelper.addDays(visit.getVisitDateTime(), ContactLogic.ALLOWED_CONTACT_DATE_OFFSET + 1));
		
		assertThat(getContactService().getAllByVisit(visitEntity), hasSize(2));
		
		// Adding another contact that is compatible to the time frame, but has a different person and/or disease should not increase the collection size
		PersonDto person2 = creator.createPerson();
		creator.createContact(user.toReference(), person2.toReference());
		creator.createContact(user.toReference(), person.toReference(), Disease.CSM);
		
		assertThat(getContactService().getAllByVisit(visitEntity), hasSize(2));
		
		// Changing the contact disease should decrease the collection size
		contact2.setDisease(Disease.CSM);
		getContactFacade().saveContact(contact2);
		
		assertThat(getContactService().getAllByVisit(visitEntity), hasSize(1));
	}
}
