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
import java.util.Calendar;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.commons.selection.Limit;
import org.everit.commons.selection.LimitedResult;
import org.everit.commons.selection.util.CalendarRange;
import org.everit.osgi.balance.api.BalanceAccount;
import org.everit.osgi.balance.api.BalanceAccountService;
import org.everit.osgi.balance.api.BalanceTransfer;
import org.everit.osgi.balance.api.BalanceTransferService;
import org.everit.osgi.balance.api.TransferFilter;
import org.everit.osgi.balance.api.TransferOrder;
import org.everit.osgi.balance.api.TransferStatus;
import org.everit.osgi.balance.api.exception.BalanceAccountNotFoundException;
import org.everit.osgi.balance.api.exception.InactiveCreditorException;
import org.everit.osgi.balance.api.exception.InactiveDebtorException;
import org.everit.osgi.balance.api.exception.InvalidTransferOperationException;
import org.everit.osgi.balance.api.exception.NonExistentTransferException;
import org.everit.osgi.balance.api.exception.SameAccountsException;
import org.everit.osgi.balance.ri.schema.Validation;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.junit.Assert;
import org.junit.Test;

@Component(name = "BalanceTransferServiceTest", immediate = true, configurationFactory = false,
        policy = ConfigurationPolicy.OPTIONAL)
@Properties({
        @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE, value = "junit4"),
        @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID, value = "BalanceTransferServiceTest"),
        @Property(name = "balanceAccountService.target"),
        @Property(name = "balanceTransferService.target"),
        @Property(name = "databaseTestHelper.target")
})
@Service(value = BalanceTransferServiceTestCompnent.class)
@TestDuringDevelopment
public class BalanceTransferServiceTestCompnent {

    public static final String TRANSFER_CODE = "transferCode";

    public static final String NOTES = "notes";

    public static final BigDecimal AMOUNT = BigDecimal.valueOf(400);

    private static final double PRECISION = Math.pow(10, -4);

    private static final TransferOrder ORDER = TransferOrder.TRANSFER_ID_ASC;

    private static final long NONEXISTENT_TRANSFER_ID = -666;

    private static final long NONEXISTENT_ACCOUNT_ID = -666;

    private static final BigDecimal ZERO = BigDecimal.valueOf(0, Validation.BALANCE_SCALE);

    public static final Calendar TOMORROW;

    public static final Calendar TODAY;

    public static final Calendar YESTERDAY;

    @Reference
    private BalanceAccountService balanceAccountService;

    @Reference
    private BalanceTransferService balanceTransferService;

    @Reference
    private DatabaseTestHelper databaseTestHelper;

    private long creditorAccountId;

    private long debtorAccountId;

    static {
        YESTERDAY = Calendar.getInstance();
        YESTERDAY.add(Calendar.HOUR, -24);
        YESTERDAY.getTime();

        TODAY = Calendar.getInstance();

        TOMORROW = Calendar.getInstance();
        TOMORROW.add(Calendar.HOUR, 24);
    }

    public void bindBalanceAccountService(final BalanceAccountService balanceAccountService) {
        this.balanceAccountService = balanceAccountService;
    }

    public void bindBalanceTransferService(final BalanceTransferService balanceTransferService) {
        this.balanceTransferService = balanceTransferService;
    }

    public void bindDatabaseTestHelper(final DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    private long createBlockedTransfer() {
        return balanceTransferService.createBlockedTransfer(TRANSFER_CODE, creditorAccountId,
                debtorAccountId, AMOUNT, NOTES);
    }

    private long createInstantTransfer() {
        return balanceTransferService.createInstantTransfer(TRANSFER_CODE, creditorAccountId,
                debtorAccountId, AMOUNT, NOTES);
    }

    private void initDb() {
        databaseTestHelper.cleanDb();
        creditorAccountId = balanceAccountService.createAccount(null, true);
        debtorAccountId = balanceAccountService.createAccount(null, true);
    }

    @Test
    public void testAcceptBlockedTransfer() {
        initDb();

        long transferId = createBlockedTransfer();
        balanceTransferService.acceptBlockedTransfer(transferId);

        BalanceTransfer balanceTransfer = balanceTransferService.findTransferById(transferId);

        long creditorAccountId = balanceTransfer.getCreditorAccountId();
        long debtorAccountId = balanceTransfer.getDebtorAccountId();

        Assert.assertEquals(TransferStatus.SUCCESSFUL, balanceTransfer.getTransferStatus());
        Assert.assertNotNull(balanceTransfer.getCreatedAt());
        Assert.assertNotNull(balanceTransfer.getAccomplishedAt());
        Assert.assertEquals(this.creditorAccountId, creditorAccountId);
        Assert.assertEquals(this.debtorAccountId, debtorAccountId);

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);

        Assert.assertEquals(AMOUNT.negate().doubleValue(), creditorAccount.getAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), creditorAccount.getBlockedBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(AMOUNT.doubleValue(), debtorAccount.getAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getBlockedBalance().doubleValue(),
                PRECISION);
    }

    @Test
    public void testCreateBlockedTransfer() {
        initDb();

        long transferId = createBlockedTransfer();

        BalanceTransfer balanceTransfer = balanceTransferService.findTransferById(transferId);
        Assert.assertNotNull(balanceTransfer);

        long creditorAccountId = balanceTransfer.getCreditorAccountId();
        long debtorAccountId = balanceTransfer.getDebtorAccountId();

        Assert.assertEquals(this.creditorAccountId, creditorAccountId);
        Assert.assertEquals(this.debtorAccountId, debtorAccountId);

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);

        Assert.assertEquals(AMOUNT.negate().doubleValue(), creditorAccount.getAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(AMOUNT.doubleValue(), creditorAccount.getBlockedBalance().doubleValue(),
                PRECISION);

        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getBlockedBalance().doubleValue(),
                PRECISION);

        Assert.assertEquals(TRANSFER_CODE, balanceTransfer.getTransferCode());
        Assert.assertEquals(TransferStatus.BLOCKED, balanceTransfer.getTransferStatus());
        Assert.assertNull(balanceTransfer.getAccomplishedAt());
        Assert.assertNotNull(balanceTransfer.getCreatedAt());
        Assert.assertEquals(NOTES, balanceTransfer.getNotes());

        Assert.assertEquals(AMOUNT.negate().doubleValue(), balanceTransfer.getAmount().doubleValue(),
                PRECISION);
        Assert.assertEquals(AMOUNT.negate().doubleValue(), balanceTransfer.getLastCreditorAvailableBalance()
                .doubleValue(), PRECISION);
        Assert.assertEquals(AMOUNT.doubleValue(), balanceTransfer.getLastCreditorBlockedBalance().doubleValue(),
                PRECISION);
    }

    @Test
    public void testCreateInstantTransfer() {
        initDb();

        long transferId = balanceTransferService.createInstantTransfer(
                TRANSFER_CODE, creditorAccountId, debtorAccountId, AMOUNT, NOTES);

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);

        Assert.assertEquals(AMOUNT.negate().doubleValue(), creditorAccount.getAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), creditorAccount.getBlockedBalance().doubleValue(), PRECISION);

        Assert.assertEquals(AMOUNT.doubleValue(), debtorAccount.getAvailableBalance().doubleValue(), PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getBlockedBalance().doubleValue(), PRECISION);

        BalanceTransfer creditorTransfer = balanceTransferService.findTransferById(transferId);

        Assert.assertEquals(TRANSFER_CODE, creditorTransfer.getTransferCode());
        Assert.assertEquals(TransferStatus.SUCCESSFUL, creditorTransfer.getTransferStatus());
        Assert.assertNotNull(creditorTransfer.getAccomplishedAt());
        Assert.assertNotNull(creditorTransfer.getCreatedAt());
        Assert.assertEquals(NOTES, creditorTransfer.getNotes());

        Assert.assertEquals(creditorAccount.getAccountId(), creditorTransfer.getCreditorAccountId());
        Assert.assertEquals(debtorAccount.getAccountId(), creditorTransfer.getDebtorAccountId());

        Assert.assertEquals(AMOUNT.negate().doubleValue(), creditorTransfer.getAmount().doubleValue(), PRECISION);
        Assert.assertEquals(AMOUNT.negate().doubleValue(), creditorTransfer.getLastCreditorAvailableBalance()
                .doubleValue(), PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), creditorTransfer.getLastCreditorBlockedBalance().doubleValue(),
                PRECISION);

        String transferPairId = creditorTransfer.getTransferPairId();
        BalanceTransfer[] transferPair = balanceTransferService.findTransfersByPairId(transferPairId);
        Assert.assertNotNull(transferPair);
        Assert.assertEquals(2, transferPair.length);

        creditorTransfer = transferPair[0];
        Assert.assertEquals(transferId, creditorTransfer.getTransferId());

        BalanceTransfer debtorTransfer = transferPair[1];

        Assert.assertEquals(TRANSFER_CODE, debtorTransfer.getTransferCode());
        Assert.assertEquals(TransferStatus.SUCCESSFUL, debtorTransfer.getTransferStatus());
        Assert.assertNotNull(debtorTransfer.getAccomplishedAt());
        Assert.assertNotNull(debtorTransfer.getCreatedAt());
        Assert.assertEquals(NOTES, debtorTransfer.getNotes());

        Assert.assertEquals(debtorAccount.getAccountId(), debtorTransfer.getCreditorAccountId());
        Assert.assertEquals(creditorAccount.getAccountId(), debtorTransfer.getDebtorAccountId());

        Assert.assertEquals(AMOUNT.doubleValue(), debtorTransfer.getAmount().doubleValue(), PRECISION);
        Assert.assertEquals(AMOUNT.doubleValue(), debtorTransfer.getLastCreditorAvailableBalance().doubleValue(),
                PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorTransfer.getLastCreditorBlockedBalance().doubleValue(), PRECISION);
    }

    @Test
    public void testErrorAcceptBlockedTransfer() {
        initDb();

        try {
            balanceTransferService.acceptBlockedTransfer(NONEXISTENT_TRANSFER_ID);
            Assert.fail("did not throw exception for nonexistent transfer id");
        } catch (NonExistentTransferException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_TRANSFER_ID)));
        }

        long transferId = createBlockedTransfer();
        balanceAccountService.deactivateAccount(creditorAccountId);
        try {
            balanceTransferService.acceptBlockedTransfer(transferId);
            Assert.fail("did not throw exception for inactive creditor");
        } catch (InactiveCreditorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(creditorAccountId)));
        }
        balanceAccountService.activateAccount(creditorAccountId);

        transferId = createBlockedTransfer();
        balanceAccountService.deactivateAccount(debtorAccountId);
        try {
            balanceTransferService.acceptBlockedTransfer(transferId);
            Assert.fail("did not throw exception for inactive debtor");
        } catch (InactiveDebtorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(debtorAccountId)));
        }
        balanceAccountService.activateAccount(debtorAccountId);

        transferId = createBlockedTransfer();
        balanceTransferService.acceptBlockedTransfer(transferId);
        try {
            balanceTransferService.acceptBlockedTransfer(transferId);
            Assert.fail("did not throw exception for accepting transfer in SUCCESSFUL state");
        } catch (InvalidTransferOperationException e) {
            Assert.assertTrue(e.getMessage().contains(TransferStatus.SUCCESSFUL.toString()));
        }

        transferId = createBlockedTransfer();
        balanceTransferService.rejectBlockedTransfer(transferId);
        try {
            balanceTransferService.acceptBlockedTransfer(transferId);
            Assert.fail("did not throw exception for accepting transfer in REJECTED state");
        } catch (InvalidTransferOperationException e) {
            Assert.assertTrue(e.getMessage().contains(TransferStatus.REJECTED.toString()));
        }
    }

    @Test
    public void testErrorCreateBlockedTransfer() {
        initDb();

        try {
            balanceTransferService.createBlockedTransfer(
                    TRANSFER_CODE, creditorAccountId, debtorAccountId, AMOUNT.negate(), NOTES);
            Assert.fail("did not throw exception for non positive transferred credit");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("amount must be positive", e.getMessage());
        }

        try {
            balanceTransferService.createBlockedTransfer(TRANSFER_CODE, NONEXISTENT_ACCOUNT_ID,
                    debtorAccountId, AMOUNT, NOTES);
            Assert.fail("did not throw exception for nonexistent creditor account id");
        } catch (BalanceAccountNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_ACCOUNT_ID)));
        }

        try {
            balanceTransferService.createBlockedTransfer(TRANSFER_CODE, creditorAccountId,
                    NONEXISTENT_ACCOUNT_ID, AMOUNT, NOTES);
            Assert.fail("did not throw exception for nonexistent debtor account id");
        } catch (BalanceAccountNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_ACCOUNT_ID)));
        }

        balanceAccountService.deactivateAccount(creditorAccountId);
        try {
            createBlockedTransfer();
            Assert.fail("did not throw exception for inactive creditor account");
        } catch (InactiveCreditorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(creditorAccountId)));
        }
        balanceAccountService.activateAccount(creditorAccountId);

        balanceAccountService.deactivateAccount(debtorAccountId);
        try {
            createBlockedTransfer();
            Assert.fail("did not throw exception for inactive debtor account");
        } catch (InactiveDebtorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(debtorAccountId)));
        }

    }

    @Test
    public void testErrorCreateInstantTransfer() {
        initDb();

        try {
            balanceTransferService.createInstantTransfer(TRANSFER_CODE, creditorAccountId,
                    debtorAccountId, AMOUNT.negate(), NOTES);
            Assert.fail("did not throw exception for non positive transferred credit");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("amount must be positive", e.getMessage());
        }

        try {
            balanceTransferService.createInstantTransfer(TRANSFER_CODE, NONEXISTENT_ACCOUNT_ID,
                    debtorAccountId, AMOUNT, NOTES);
            Assert.fail("did not throw exception for nonexistent creditor account id");
        } catch (BalanceAccountNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_ACCOUNT_ID)));
        }

        try {
            balanceTransferService.createInstantTransfer(TRANSFER_CODE, creditorAccountId,
                    NONEXISTENT_ACCOUNT_ID,
                    AMOUNT, NOTES);
            Assert.fail("did not throw exception for nonexistent debtor account id");
        } catch (BalanceAccountNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_ACCOUNT_ID)));
        }

        balanceAccountService.deactivateAccount(creditorAccountId);
        try {
            createInstantTransfer();
            Assert.fail("did not throw exception for inactive creditor account");
        } catch (InactiveCreditorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(creditorAccountId)));
        }
        balanceAccountService.activateAccount(creditorAccountId);

        balanceAccountService.deactivateAccount(debtorAccountId);
        try {
            createInstantTransfer();
            Assert.fail("did not throw exception for inactive debtor account");
        } catch (InactiveDebtorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(debtorAccountId)));
        }
    }

    @Test
    public void testErrorRejectBlockedTransfer() {
        initDb();

        try {
            balanceTransferService.rejectBlockedTransfer(NONEXISTENT_TRANSFER_ID);
            Assert.fail("did not throw exception for nonexistent transfer id");
        } catch (NonExistentTransferException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(NONEXISTENT_TRANSFER_ID)));
        }

        long transferId = createBlockedTransfer();
        balanceTransferService.acceptBlockedTransfer(transferId);
        try {
            balanceTransferService.rejectBlockedTransfer(transferId);
            Assert.fail("did not throw exception for rejecting a transfer which is not in BLOCKED state");
        } catch (InvalidTransferOperationException e) {
            Assert.assertTrue(e.getMessage().contains(TransferStatus.SUCCESSFUL.toString()));
        }

        transferId = createBlockedTransfer();
        balanceTransferService.rejectBlockedTransfer(transferId);
        try {
            balanceTransferService.rejectBlockedTransfer(transferId);
            Assert.fail("did not throw exception for rejecting a transfer which is not in BLOCKED state");
        } catch (InvalidTransferOperationException e) {
            Assert.assertTrue(e.getMessage().contains(TransferStatus.REJECTED.toString()));
        }

        transferId = createBlockedTransfer();
        balanceAccountService.deactivateAccount(creditorAccountId);
        try {
            balanceTransferService.rejectBlockedTransfer(transferId);
            Assert.fail("did not throw exception for rejecting transfer with inactive creditor account");
        } catch (InactiveCreditorException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(creditorAccountId)));
        }
        balanceAccountService.activateAccount(creditorAccountId);
    }

    @Test
    public void testErrorSameAccount() {
        initDb();

        long accountId = creditorAccountId;
        try {
            balanceTransferService
                    .createInstantTransfer(TRANSFER_CODE, accountId, accountId, AMOUNT, NOTES);
            Assert.fail();
        } catch (SameAccountsException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(accountId)));
        }
        try {
            balanceTransferService
                    .createBlockedTransfer(TRANSFER_CODE, accountId, accountId, AMOUNT, NOTES);
            Assert.fail();
        } catch (SameAccountsException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(accountId)));
        }
    }

    @Test
    public void testFindTransfersByAccomplishedAt() {
        initDb();

        databaseTestHelper.createDummyTransfers();
        LimitedResult<BalanceTransfer> result = balanceTransferService.findTransfers(
                new TransferFilter(null, null, null, new CalendarRange(TOMORROW, TOMORROW), null, null),
                ORDER, new Limit(1, 10));

        Assert.assertEquals(40, result.getNumberOfAllElements());
    }

    @Test
    public void testFindTransfersByCreatedAt() {
        initDb();

        databaseTestHelper.createDummyTransfers();
        LimitedResult<BalanceTransfer> result = balanceTransferService.findTransfers(
                new TransferFilter(null, null, new CalendarRange(YESTERDAY, YESTERDAY), null, null, null),
                ORDER, new Limit(1, 10));

        Assert.assertEquals(40, result.getNumberOfAllElements());
    }

    @Test
    public void testFindTransfersByCreditor() {
        initDb();

        long[] accountIds = databaseTestHelper.createDummyTransfers();
        long creditorAccountId = accountIds[0];
        long debtorAccountId = accountIds[1];

        Limit limit = new Limit(0, 22);
        LimitedResult<BalanceTransfer> result = balanceTransferService.findTransfers(
                new TransferFilter(creditorAccountId, null, null, null, null, null),
                ORDER, limit);

        Assert.assertEquals(50, result.getNumberOfAllElements());
        Assert.assertEquals(limit, result.getLimit());

        List<BalanceTransfer> elements = result.getElements();
        Assert.assertEquals(22, elements.size());

        for (int i = 0; i < 20; i++) {
            BalanceTransfer transfer = elements.get(i);
            Assert.assertEquals("" + i, creditorAccountId, transfer.getCreditorAccountId());
            Assert.assertEquals("" + i, debtorAccountId, transfer.getDebtorAccountId());
            Assert.assertEquals("" + i, YESTERDAY, transfer.getCreatedAt());
            Assert.assertEquals("" + i, TODAY, transfer.getAccomplishedAt());
        }
        for (int i = 20; i < 22; i++) {
            BalanceTransfer transfer = elements.get(i);
            Assert.assertEquals("" + i, creditorAccountId, transfer.getCreditorAccountId());
            Assert.assertEquals("" + i, debtorAccountId, transfer.getDebtorAccountId());
            Assert.assertEquals("" + i, TODAY, transfer.getCreatedAt());
            Assert.assertEquals("" + i, TODAY, transfer.getAccomplishedAt());
        }
    }

    @Test
    public void testFindTransfersByDebtor() {
        initDb();

        long[] accountIds = databaseTestHelper.createDummyTransfers();
        long creditorAccountId = accountIds[0];
        long debtorAccountId = accountIds[1];

        Limit limit = new Limit(0, 22);
        LimitedResult<BalanceTransfer> result = balanceTransferService.findTransfers(
                new TransferFilter(null, debtorAccountId, null, null, null, null),
                ORDER, limit);

        Assert.assertEquals(50, result.getNumberOfAllElements());
        Assert.assertEquals(limit, result.getLimit());

        List<BalanceTransfer> elements = result.getElements();
        Assert.assertEquals(22, elements.size());

        for (int i = 0; i < 20; i++) {
            BalanceTransfer transfer = elements.get(i);
            Assert.assertEquals("" + i, creditorAccountId, transfer.getCreditorAccountId());
            Assert.assertEquals("" + i, debtorAccountId, transfer.getDebtorAccountId());
            Assert.assertEquals("" + i, YESTERDAY, transfer.getCreatedAt());
            Assert.assertEquals("" + i, TODAY, transfer.getAccomplishedAt());
        }
        for (int i = 20; i < 22; i++) {
            BalanceTransfer transfer = elements.get(i);
            Assert.assertEquals("" + i, creditorAccountId, transfer.getCreditorAccountId());
            Assert.assertEquals("" + i, debtorAccountId, transfer.getDebtorAccountId());
            Assert.assertEquals("" + i, TODAY, transfer.getCreatedAt());
            Assert.assertEquals("" + i, TODAY, transfer.getAccomplishedAt());
        }
    }

    @Test
    public void testRejectBlockedTransfer() {
        initDb();

        long transferId = createBlockedTransfer();
        balanceTransferService.rejectBlockedTransfer(transferId);
        BalanceTransfer transfer = balanceTransferService.findTransferById(transferId);

        long creditorAccountId = transfer.getCreditorAccountId();
        long debtorAccountId = transfer.getDebtorAccountId();

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);

        Assert.assertEquals(TransferStatus.REJECTED, transfer.getTransferStatus());
        Assert.assertNotNull(transfer.getCreatedAt());
        Assert.assertNotNull(transfer.getAccomplishedAt());
        Assert.assertEquals(creditorAccountId, creditorAccount.getAccountId());
        Assert.assertEquals(debtorAccountId, debtorAccount.getAccountId());

        Assert.assertEquals(ZERO.doubleValue(), creditorAccount.getAvailableBalance().doubleValue(), PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), creditorAccount.getBlockedBalance().doubleValue(), PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getAvailableBalance().doubleValue(), PRECISION);
        Assert.assertEquals(ZERO.doubleValue(), debtorAccount.getBlockedBalance().doubleValue(), PRECISION);
    }

}
