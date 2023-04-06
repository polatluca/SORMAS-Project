/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.backend.sormastosormas.rest;

import javax.enterprise.inject.Produces;

import de.symeda.sormas.backend.common.ConfigFacadeEjb;
import de.symeda.sormas.backend.sormastosormas.access.SormasToSormasDiscoveryService;
import de.symeda.sormas.backend.sormastosormas.crypto.SormasToSormasEncryptionFacadeEjb.SormasToSormasEncryptionFacadeEjbLocal;

public class SormasToSormasRestClientProducer {

	@Produces
	public SormasToSormasRestClient sormasToSormasClient(
		SormasToSormasDiscoveryService sormasToSormasDiscoveryService,
		SormasToSormasEncryptionFacadeEjbLocal sormasToSormasEncryptionEjb,
		ConfigFacadeEjb.ConfigFacadeEjbLocal configFacadeEjb) {
		return new SormasToSormasRestClient(sormasToSormasDiscoveryService, sormasToSormasEncryptionEjb, configFacadeEjb);
	}
}
