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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.balance.api.BalanceAccountService;
import org.everit.osgi.balance.api.BalanceTransferService;
import org.everit.osgi.balance.api.TransferStatus;
import org.everit.osgi.balance.ri.schema.qdsl.QBalanceAccount;
import org.everit.osgi.balance.ri.schema.qdsl.QBalanceTransfer;
import org.everit.osgi.resource.api.ResourceService;
import org.everit.osgi.transaction.helper.api.Callback;
import org.everit.osgi.transaction.helper.api.TransactionHelper;

import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;

@Component(name = "DatabaseTestHelper", immediate = true, configurationFactory = false,
        policy = ConfigurationPolicy.OPTIONAL)
@Properties({
        @Property(name = "transactionHelper.target"),
        @Property(name = "dataSource.target"),
        @Property(name = "sqlTemplates.target"),
        @Property(name = "balanceAccountService.target"),
        @Property(name = "balanceTransferService.target"),
        @Property(name = "resourceService.target")
})
@Service
public class DatabaseTestHelperComponent implements DatabaseTestHelper {

    @Reference
    private TransactionHelper transactionHelper;

    @Reference
    private DataSource dataSource;

    @Reference
    private SQLTemplates sqlTemplates;

    @Reference
    private BalanceAccountService balanceAccountService;

    @Reference
    private BalanceTransferService balanceTransferService;

    @Reference
    private ResourceService resourceService;

    public void bindBalanceAccountService(final BalanceAccountService balanceAccountService) {
        this.balanceAccountService = balanceAccountService;
    }

    public void bindBalanceTransferService(final BalanceTransferService balanceTransferService) {
        this.balanceTransferService = balanceTransferService;
    }

    public void bindDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void bindResourceService(final ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public void bindSqlTemplates(final SQLTemplates sqlTemplates) {
        this.sqlTemplates = sqlTemplates;
    }

    public void bindTransactionHelper(final TransactionHelper transactionHelper) {
        this.transactionHelper = transactionHelper;
    }

    @Override
    public void cleanDb() {
        transactionHelper.required(new Callback<Object>() {

            @Override
            public Object execute() {
                QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
                QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                try (Connection connection = dataSource.getConnection()) {
                    new SQLDeleteClause(connection, sqlTemplates, qBalanceTransfer).execute();
                    new SQLDeleteClause(connection, sqlTemplates, qBalanceAccount).execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

        });
    }

    @Override
    public long[] createDummyTransfers() {
        return transactionHelper.required(new Callback<long[]>() {

            @Override
            public long[] execute() {
                long creditorAccountId = balanceAccountService.createAccount(null, true);
                long debtorAccountId = balanceAccountService.createAccount(null, true);

                createTransferList(20, creditorAccountId, debtorAccountId,
                        BalanceTransferServiceTestCompnent.YESTERDAY, BalanceTransferServiceTestCompnent.TODAY);
                createTransferList(10, creditorAccountId, debtorAccountId,
                        BalanceTransferServiceTestCompnent.TODAY, BalanceTransferServiceTestCompnent.TODAY);
                createTransferList(20, creditorAccountId, debtorAccountId,
                        BalanceTransferServiceTestCompnent.TODAY, BalanceTransferServiceTestCompnent.TOMORROW);

                return new long[] { creditorAccountId, debtorAccountId };
            };

        });
    }

    private void createTransfer(final String transferPairId, final long creditorAccountId, final long debtorAccountId,
            final Calendar createdAt,
            final Calendar accomplishedAt, final BigDecimal amount) {
        long resourceId = resourceService.createResource();
        QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
        try (Connection connection = dataSource.getConnection()) {
            new SQLInsertClause(connection, sqlTemplates, qBalanceTransfer)
                    .set(qBalanceTransfer.transferCode, BalanceTransferServiceTestCompnent.TRANSFER_CODE)
                    .set(qBalanceTransfer.transferPairId, transferPairId)
                    .set(qBalanceTransfer.creditorAccountId, creditorAccountId)
                    .set(qBalanceTransfer.debtorAccountId, debtorAccountId)
                    .set(qBalanceTransfer.amount, amount.doubleValue())
                    .set(qBalanceTransfer.lastCreditorAvailableBalance, BigDecimal.ZERO.doubleValue())
                    .set(qBalanceTransfer.lastCreditorBlockedBalance, BigDecimal.ZERO.doubleValue())
                    .set(qBalanceTransfer.transferStatus, TransferStatus.SUCCESSFUL.name())
                    .set(qBalanceTransfer.createdAt, new Timestamp(createdAt.getTimeInMillis()))
                    .set(qBalanceTransfer.accomplishedAt, new Timestamp(accomplishedAt.getTimeInMillis()))
                    .set(qBalanceTransfer.notes, BalanceTransferServiceTestCompnent.NOTES)
                    .set(qBalanceTransfer.resourceId, resourceId)
                    .execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTransferList(final int count, final long creditorAccountId, final long debtorAccountId,
            final Calendar createdAt, final Calendar accomplishedAt) {
        for (int i = 0; i < count; i++) {
            String transferPairId = UUID.randomUUID().toString().replaceAll("-", "");
            createTransfer(transferPairId, creditorAccountId, debtorAccountId, createdAt, accomplishedAt,
                    BalanceTransferServiceTestCompnent.AMOUNT);
            createTransfer(transferPairId, debtorAccountId, creditorAccountId, createdAt, accomplishedAt,
                    BalanceTransferServiceTestCompnent.AMOUNT.negate());
        }
    }

}
