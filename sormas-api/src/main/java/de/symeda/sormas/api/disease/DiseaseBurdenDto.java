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
package de.symeda.sormas.api.disease;

import java.io.Serializable;

import de.symeda.sormas.api.Disease;

public class DiseaseBurdenDto  implements Serializable {
	
	private static final long serialVersionUID = 2430932452606853497L;
	
	public static final String I18N_PREFIX = "DiseaseBurden";
	
	public static final String DISEASE = "disease";
	public static final String CASE_COUNT = "caseCount";
	public static final String PREVIOUS_CASE_COUNT = "previousCaseCount";
	public static final String CASES_DIFFERENCE = "casesDifference";
	public static final String EVENT_COUNT = "eventCount";
	public static final String OUTBREAK_DISTRICT_COUNT = "outbreakDistrictCount";
	public static final String CASE_DEATH_COUNT = "caseDeathCount";
	public static final String CASE_FATALITY_RATE = "caseFatalityRate";
	public static final String LAST_REPORTED_COMMUNITY_NAME = "lastReportedCommunityName";
	
	private Disease disease;
	private Long caseCount;
	private Long previousCaseCount;
	private Long eventCount;
	private Long outbreakDistrictCount;
	private Long caseDeathCount;
	private String lastReportedCommunityName;
	
	public DiseaseBurdenDto(Disease disease, Long caseCount, Long previousCaseCount, Long eventCount, Long outbreakDistrictCount, Long caseDeathCount, String lastReportedCommunityName) {
		this.disease = disease;
		this.caseCount = caseCount;
		this.previousCaseCount = previousCaseCount;
		this.eventCount = eventCount;
		this.outbreakDistrictCount = outbreakDistrictCount;
		this.caseDeathCount = caseDeathCount;
		this.lastReportedCommunityName = lastReportedCommunityName;
	}
	
	public Disease getDisease() {
		return disease;
	}
	public void setDisease(Disease disease) {
		this.disease = disease;
	}
	
	public Long getCaseCount() {
		return caseCount;
	}
	public void setCaseCount(Long caseCount) {
		this.caseCount = caseCount;
	}
	
	public Long getPreviousCaseCount() {
		return previousCaseCount;
	}
	public void setPreviousCaseCount(Long previousCaseCount) {
		this.previousCaseCount = previousCaseCount;
	}
	
	public Long getCasesDifference() {
		return getCaseCount() - getPreviousCaseCount();
	}
	
	public Long getEventCount() {
		return eventCount;
	}
	public void setEventCount(Long eventCount) {
		this.eventCount = eventCount;
	}
	
	public Long getOutbreakDistrictCount() {
		return outbreakDistrictCount;
	}
	public void setOutbreakDistrictCount(Long outbreakDistrictCount) {
		this.outbreakDistrictCount = outbreakDistrictCount;
	}
	
	public Long getCaseDeathCount() {
		return caseDeathCount;
	}
	public void setCaseDeathCount(Long caseDeathCount) {
		this.caseDeathCount = caseDeathCount;
	}
	
	public float getCaseFatalityRate() {
		float cfrPercentage = 100f * ((float)getCaseDeathCount() / (float)(getCaseCount() == 0 ? 1 : getCaseCount()));
		cfrPercentage = Math.round(cfrPercentage * 100) / 100f;
		return cfrPercentage;
	}
	
	public String getLastReportedCommunityName() {
		return lastReportedCommunityName;
	}
	public void setLastReportedCommunityName(String name) {
		this.lastReportedCommunityName = name;
	}
	
	public Boolean hasCount () {
		return (caseCount + previousCaseCount + eventCount + outbreakDistrictCount) > 0;
	}
}
