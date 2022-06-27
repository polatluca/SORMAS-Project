/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package org.sormas.e2etests.steps.web.application.dashboard.contacts;

import static org.sormas.e2etests.pages.application.dashboard.Contacts.ContactsDashboardPage.CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD;
import static org.sormas.e2etests.pages.application.dashboard.Contacts.ContactsDashboardPage.CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD_DE;
import static org.sormas.e2etests.pages.application.dashboard.Contacts.ContactsDashboardPage.CONFIRMED_COUNTER_ON_CONTACTS_DASHBOARD;
import static org.sormas.e2etests.pages.application.dashboard.Contacts.ContactsDashboardPage.CONFIRMED_COUNTER_ON_CONTACTS_DASHBOARD_DE;
import static org.sormas.e2etests.pages.application.dashboard.Contacts.ContactsDashboardPage.UNDER_FU_CHART_ON_CONTACTS_DASHBOARD;
import static org.sormas.e2etests.pages.application.dashboard.Surveillance.ContactsDashboardPage.CONTACTS_COVID19_COUNTER;

import cucumber.api.java8.En;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.sormas.e2etests.helpers.WebDriverHelpers;
import org.testng.Assert;

public class ContactsDashboardSteps implements En {

  private final WebDriverHelpers webDriverHelpers;
  private int covid19ContactsCounterAfter;
  private int covid19ContactsCounterBefore;
  public static String confirmedContact_EN;
  public static String confirmedContact_DE;

  @Inject
  public ContactsDashboardSteps(WebDriverHelpers webDriverHelpers) {
    this.webDriverHelpers = webDriverHelpers;

    When(
        "^I save value for COVID-19 contacts counter in Contacts Dashboard$",
        () -> {
          webDriverHelpers.waitForPageLoadingSpinnerToDisappear(60);
          webDriverHelpers.waitUntilIdentifiedElementIsVisibleAndClickable(
              CONTACTS_COVID19_COUNTER, 40);
          covid19ContactsCounterBefore =
              Integer.parseInt(webDriverHelpers.getTextFromWebElement(CONTACTS_COVID19_COUNTER));
        });

    Then(
        "^I check that previous saved Contacts Dashboard contact counter for COVID-19 has been incremented$",
        () -> {
          webDriverHelpers.waitForPageLoadingSpinnerToDisappear(60);
          webDriverHelpers.waitUntilIdentifiedElementIsPresent(CONTACTS_COVID19_COUNTER);
          // TODO check if this sleep helps for Jenkins execution, otherwise remove it and create
          // proper handle
          TimeUnit.SECONDS.sleep(5);
          covid19ContactsCounterAfter =
              Integer.parseInt(webDriverHelpers.getTextFromWebElement(CONTACTS_COVID19_COUNTER));
          Assert.assertTrue(
              covid19ContactsCounterBefore < covid19ContactsCounterAfter,
              "COVID-19 contacts counter in Contacts dashboard hasn't  been incremented");
        });

    Then(
        "I get Confirmed Contact labels and value from Contact Dashboard with English language",
        () -> {
          webDriverHelpers.waitUntilElementIsVisibleAndClickable(
              CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD);
          webDriverHelpers.getWebElement(CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD);
          confirmedContact_EN =
              webDriverHelpers.getWebElement(CONFIRMED_COUNTER_ON_CONTACTS_DASHBOARD).getText();
          // System.out.println(confirmedContact);

          webDriverHelpers.getWebElement(UNDER_FU_CHART_ON_CONTACTS_DASHBOARD);
        });
    Then(
        "I get Confirmed Contact labels and value from Contact Dashboard with Deutsch language",
        () -> {
          webDriverHelpers.waitUntilElementIsVisibleAndClickable(
              CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD_DE);
          webDriverHelpers.getWebElement(CONFIRMED_COUNTER_LABEL_ON_CONTACTS_DASHBOARD_DE);
          confirmedContact_DE =
              webDriverHelpers.getWebElement(CONFIRMED_COUNTER_ON_CONTACTS_DASHBOARD_DE).getText();
          // System.out.println(confirmedContact);

          webDriverHelpers.getWebElement(UNDER_FU_CHART_ON_CONTACTS_DASHBOARD);
        });
    And(
        "I compare English and German confirmed contacts counter",
        () -> {
          Assert.assertEquals(confirmedContact_EN, confirmedContact_DE, "Counters not equal!");
        });
  }
}
