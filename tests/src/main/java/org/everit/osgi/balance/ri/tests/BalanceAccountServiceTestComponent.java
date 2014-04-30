/**
 * This file is part of org.everit.osgi.balance.ri.tests.
 *
 * org.everit.osgi.balance.ri.tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * org.everit.osgi.balance.ri.tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with org.everit.osgi.balance.ri.tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.balance.ri.tests;

import java.math.BigDecimal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.commons.selection.Limit;
import org.everit.commons.selection.LimitedResult;
import org.everit.osgi.balance.api.BalanceAccount;
import org.everit.osgi.balance.api.BalanceAccountService;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.resource.api.ResourceService;
import org.junit.Assert;
import org.junit.Test;

@Component(name = "BalanceAccountServiceTest", immediate = true, configurationFactory = false,
        policy = ConfigurationPolicy.OPTIONAL)
@Properties({
        @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE, value = "junit4"),
        @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID, value = "BalanceAccountServiceTest"),
        @Property(name = "balanceAccountService.target"),
        @Property(name = "resourceService.target"),
        @Property(name = "databaseTestHelper.target")
})
@Service(value = BalanceAccountServiceTestComponent.class)
@TestDuringDevelopment
public class BalanceAccountServiceTestComponent {

    private static final BigDecimal ZERO = BigDecimal.valueOf(0.0);

    @Reference
    private BalanceAccountService balanceAccountService;

    @Reference
    private ResourceService resourceService;

    @Reference
    private DatabaseTestHelper databaseTestHelper;

    public void bindBalanceAccountService(final BalanceAccountService balanceAccountService) {
        this.balanceAccountService = balanceAccountService;
    }

    public void bindDatabaseTestHelper(final DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public void bindResourceService(final ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private long[] createDummyAccounts(final int numberOfAccountsToCreate) {
        // creating 2 owner entities with 0 and 1 indices
        long[] owners = {
                resourceService.createResource(),
                resourceService.createResource()
        };
        short ownerIdx = 0;
        for (int i = 0; i < numberOfAccountsToCreate; ++i) {
            balanceAccountService.createAccount(owners[ownerIdx], true);
            // switching between 0 and 1
            ownerIdx = (short) (1 - ownerIdx);
        }
        return new long[] { owners[0], owners[1] };
    }

    @Test
    public void testActivateAccount() {
        databaseTestHelper.cleanDb();

        long accountId = balanceAccountService.createAccount(null, true);
        BalanceAccount balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertTrue(balanceAccount.isActive());

        balanceAccountService.deactivateAccount(accountId);
        balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertFalse(balanceAccount.isActive());

        balanceAccountService.activateAccount(accountId);
        balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertTrue(balanceAccount.isActive());
    }

    @Test
    public void testCreateAccount() {
        databaseTestHelper.cleanDb();

        long accountId = balanceAccountService.createAccount(null, true);
        BalanceAccount balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertEquals(accountId, balanceAccount.getAccountId());
        Assert.assertTrue(balanceAccount.isActive());
        Assert.assertEquals(ZERO, balanceAccount.getAvailableBalance());
        Assert.assertEquals(ZERO, balanceAccount.getBlockedBalance());
        Assert.assertTrue(balanceAccount.getOwnerResourceId() != 0);
        Assert.assertTrue(balanceAccount.getResourceId() != 0);

        long ownerResourceId = resourceService.createResource();
        accountId = balanceAccountService.createAccount(ownerResourceId, true);
        balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertEquals(accountId, balanceAccount.getAccountId());
        Assert.assertTrue(balanceAccount.isActive());
        Assert.assertEquals(ZERO, balanceAccount.getAvailableBalance());
        Assert.assertEquals(ZERO, balanceAccount.getBlockedBalance());
        Assert.assertEquals(ownerResourceId, balanceAccount.getOwnerResourceId());
        Assert.assertTrue(balanceAccount.getResourceId() != 0);
    }

    @Test
    public void testFindAccountById() {
        databaseTestHelper.cleanDb();

        long accountId = balanceAccountService.createAccount(null, true);
        BalanceAccount balanceAccount = balanceAccountService.findAccountById(accountId + 1);
        Assert.assertNull(balanceAccount);

        balanceAccount = balanceAccountService.findAccountById(accountId);
        Assert.assertEquals(accountId, balanceAccount.getAccountId());
    }

    @Test
    public void testFindAccountsByOwnerResourceId() {
        databaseTestHelper.cleanDb();

        createDummyAccounts(100);
        Limit limit = new Limit(2, 10);
        LimitedResult<BalanceAccount> accounts = balanceAccountService.findAccountsByOwnerResourceId(null, limit);
        Assert.assertEquals(limit, accounts.getLimit());
        Assert.assertEquals(100, accounts.getNumberOfAllElements());
        long expectedId = 0;
        for (BalanceAccount account : accounts.getElements()) {
            if (expectedId == 0) {
                expectedId = account.getAccountId();
            } else {
                ++expectedId;
            }
            Assert.assertEquals(expectedId, account.getAccountId());
        }
    }

    @Test
    public void testFindAccountsWithOwner() {
        databaseTestHelper.cleanDb();

        long[] ownerIds = createDummyAccounts(100);
        Limit limit = new Limit(2, 10);
        LimitedResult<BalanceAccount> accounts = balanceAccountService
                .findAccountsByOwnerResourceId(ownerIds[0], limit);
        Assert.assertEquals(limit, accounts.getLimit());
        Assert.assertEquals(50, accounts.getNumberOfAllElements());
        long expectedId = 0;
        for (BalanceAccount account : accounts.getElements()) {
            if (expectedId == 0) {
                expectedId = account.getAccountId();
            } else {
                expectedId += 2;
            }
            Assert.assertEquals(expectedId, account.getAccountId());
        }
    }

}
