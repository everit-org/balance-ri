/**
 * This file is part of org.everit.osgi.balance.ri.
 *
 * org.everit.osgi.balance.ri is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * org.everit.osgi.balance.ri is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with org.everit.osgi.balance.ri.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.balance.ri;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

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
import org.everit.osgi.balance.ri.internal.QdslPredicateUtil;
import org.everit.osgi.balance.ri.schema.qdsl.QBalanceAccount;
import org.everit.osgi.balance.ri.schema.qdsl.QBalanceTransfer;
import org.everit.osgi.resource.api.ResourceService;
import org.everit.osgi.transaction.helper.api.Callback;
import org.everit.osgi.transaction.helper.api.TransactionHelper;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;

/**
 * The reference implementation of the {@link BalanceTransferService}.
 */
@Component(name = BalanceTransferConstants.COMPONENT_NAME, metatype = true,
        configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = BalanceTransferConstants.PROP_TRANSACTION_HELPER),
        @Property(name = BalanceTransferConstants.PROP_DATA_SOURCE),
        @Property(name = BalanceTransferConstants.PROP_SQL_TEMPLATES),
        @Property(name = BalanceTransferConstants.PROP_RESOURCE_SERVICE_TARGET),
        @Property(name = BalanceTransferConstants.PROP_BALANCE_ACCOUNT_SERVICE_TARGET)
})
@Service
public class BalanceTransferCompnent implements BalanceTransferService {

    @Reference
    private TransactionHelper transactionHelper;

    @Reference
    private DataSource dataSource;

    @Reference
    private SQLTemplates sqlTemplates;

    @Reference
    private ResourceService resourceService;

    @Reference
    private BalanceAccountService balanceAccountService;

    @Override
    public void acceptBlockedTransfer(final long transferId) {
        transactionHelper.required(new Callback<Object>() {

            @Override
            public Object execute() {
                try (Connection connection = dataSource.getConnection()) {
                    BalanceTransfer blockedTransfer = lockTransfer(connection, transferId);

                    long creditorAccountId = blockedTransfer.getCreditorAccountId();
                    long debtorAccountId = blockedTransfer.getDebtorAccountId();

                    balanceAccountService.lockAccounts(creditorAccountId, debtorAccountId);

                    BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
                    BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);
                    if (!creditorAccount.isActive()) {
                        throw new InactiveCreditorException(
                                "cannot accomplish the operation (accept blocked transfer) on creditorAccountId ["
                                        + creditorAccountId + "]");
                    }
                    if (!debtorAccount.isActive()) {
                        throw new InactiveDebtorException(
                                "cannot accomplish the operation (accept blocked transfer) on debtorAccountId ["
                                        + debtorAccountId + "]");
                    }

                    String transferPairId = blockedTransfer.getTransferPairId();
                    String transferCode = blockedTransfer.getTransferCode();
                    BigDecimal amount = blockedTransfer.getAmount().negate();
                    String notes = blockedTransfer.getNotes();
                    Calendar now = Calendar.getInstance();
                    transfer(connection, transferPairId, transferCode,
                            debtorAccountId, creditorAccountId, amount, notes, true, now);

                    BigDecimal creditorBlockedBalance = creditorAccount.getBlockedBalance().subtract(amount);

                    QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                    new SQLUpdateClause(connection, sqlTemplates, qBalanceAccount)
                            .where(qBalanceAccount.accountId.eq(creditorAccountId))
                            .set(qBalanceAccount.blockedBalance, creditorBlockedBalance.doubleValue())
                            .execute();

                    QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
                    new SQLUpdateClause(connection, sqlTemplates, qBalanceTransfer)
                            .where(qBalanceTransfer.transferId.eq(transferId))
                            .set(qBalanceTransfer.lastCreditorBlockedBalance, creditorBlockedBalance.doubleValue())
                            .set(qBalanceTransfer.transferStatus, TransferStatus.SUCCESSFUL.name())
                            .set(qBalanceTransfer.accomplishedAt, new Timestamp(now.getTimeInMillis()))
                            .execute();

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

        });
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
    public long createBlockedTransfer(final String transferCode, final long creditorAccountId,
            final long debtorAccountId, final BigDecimal amount, final String notes) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        if (creditorAccount == null) {
            throw new BalanceAccountNotFoundException(
                    "creditor account not found by accountId [" + creditorAccountId + "]");
        }
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);
        if (debtorAccount == null) {
            throw new BalanceAccountNotFoundException(
                    "debtor account not found by accountId [" + debtorAccountId + "]");
        }

        Long rval = transactionHelper.required(new Callback<Long>() {

            @Override
            public Long execute() {
                try (Connection connection = dataSource.getConnection()) {
                    balanceAccountService.lockAccounts(creditorAccountId, debtorAccountId);
                    Calendar now = Calendar.getInstance();
                    String transferPairId = generateTransferPairId();
                    return transfer(connection, transferPairId, transferCode,
                            creditorAccountId, debtorAccountId, amount.negate(), notes, false, now);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        });
        return rval;
    }

    @Override
    public long createInstantTransfer(final String transferCode, final long creditorAccountId,
            final long debtorAccountId, final BigDecimal amount, final String notes) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        if (creditorAccount == null) {
            throw new BalanceAccountNotFoundException(
                    "creditor account not found by accountId [" + creditorAccountId + "]");
        }
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);
        if (debtorAccount == null) {
            throw new BalanceAccountNotFoundException(
                    "debtor account not found by accountId [" + debtorAccountId + "]");
        }

        Long rval = transactionHelper.required(new Callback<Long>() {

            @Override
            public Long execute() {
                try (Connection connection = dataSource.getConnection()) {
                    balanceAccountService.lockAccounts(creditorAccountId, debtorAccountId);
                    Calendar now = Calendar.getInstance();
                    String transferPairId = generateTransferPairId();
                    long creditorTransferId = transfer(connection, transferPairId, transferCode,
                            creditorAccountId, debtorAccountId, amount.negate(), notes, true, now);
                    transfer(connection, transferPairId, transferCode,
                            debtorAccountId, creditorAccountId, amount, notes, true, now);
                    return creditorTransferId;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        });
        return rval;
    }

    private OrderSpecifier<?> createOrderBySpecifier(final QBalanceTransfer qBalanceTransfer, final TransferOrder order) {
        switch (order) {
        case TRANSFER_ID_ASC:
            return qBalanceTransfer.transferId.asc();
        case TRANSFER_ID_DESC:
            return qBalanceTransfer.transferId.desc();
        case ACCOMPLISHED_ASC:
            return qBalanceTransfer.accomplishedAt.asc();
        case ACCOMPLISHED_DESC:
            return qBalanceTransfer.accomplishedAt.desc();
        case CREATED_ASC:
            return qBalanceTransfer.createdAt.asc();
        case CREATED_DESC:
            return qBalanceTransfer.createdAt.desc();
        default:
            return null;
        }
    }

    private ConstructorExpression<BalanceTransfer> createTransferExpression(
            final QBalanceTransfer qBalanceTransfer) {
        return ConstructorExpression.create(BalanceTransfer.class,
                qBalanceTransfer.transferId,
                qBalanceTransfer.transferPairId,
                qBalanceTransfer.createdAt,
                qBalanceTransfer.accomplishedAt,
                qBalanceTransfer.transferCode,
                qBalanceTransfer.amount,
                qBalanceTransfer.transferStatus,
                qBalanceTransfer.lastCreditorAvailableBalance,
                qBalanceTransfer.lastCreditorBlockedBalance,
                qBalanceTransfer.notes,
                qBalanceTransfer.creditorAccountId,
                qBalanceTransfer.debtorAccountId,
                qBalanceTransfer.resourceId);
    }

    private Predicate[] createWherePredicate(final QBalanceTransfer qBalanceTransfer, final TransferFilter filter) {
        TransferStatus transferStatus = filter.getTransferStatus();
        return new Predicate[] {
                QdslPredicateUtil.between(qBalanceTransfer.accomplishedAt, filter.getAccomplishedRange()),
                QdslPredicateUtil.between(qBalanceTransfer.createdAt, filter.getCreatedRange()),
                QdslPredicateUtil.eq(qBalanceTransfer.creditorAccountId, filter.getCreditorAccountId()),
                QdslPredicateUtil.eq(qBalanceTransfer.debtorAccountId, filter.getDebtorAccountId()),
                QdslPredicateUtil.eq(qBalanceTransfer.transferCode, filter.getTransferCode()),
                QdslPredicateUtil.eq(qBalanceTransfer.transferStatus,
                        transferStatus == null ? null : transferStatus.name())
        };
    }

    @Override
    public BalanceTransfer findTransferById(final long transferId) {
        try (Connection connection = dataSource.getConnection()) {

            QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;

            List<BalanceTransfer> balanceTransfers = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceTransfer)
                    .where(qBalanceTransfer.transferId.eq(transferId))
                    .limit(1)
                    .list(createTransferExpression(qBalanceTransfer));
            if (balanceTransfers.isEmpty()) {
                return null;
            } else {
                return balanceTransfers.get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LimitedResult<BalanceTransfer> findTransfers(final TransferFilter filter, final TransferOrder order,
            final Limit limit) {
        if (filter == null) {
            throw new NullPointerException("filter cannot be null");
        }
        if (order == null) {
            throw new NullPointerException("order cannot be null");
        }
        if (limit == null) {
            throw new NullPointerException("limit cannot be null");
        }

        try (Connection connection = dataSource.getConnection()) {

            QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;

            long numberOfAllElements = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceTransfer)
                    .where(createWherePredicate(qBalanceTransfer, filter))
                    .count();
            if (numberOfAllElements == 0) {
                return new LimitedResult<BalanceTransfer>(new ArrayList<BalanceTransfer>(), numberOfAllElements, limit);
            }

            List<BalanceTransfer> transfers = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceTransfer)
                    .where(createWherePredicate(qBalanceTransfer, filter))
                    .orderBy(createOrderBySpecifier(qBalanceTransfer, order))
                    .limit(limit.getMaxResults())
                    .offset(limit.getFirstResult())
                    .list(createTransferExpression(qBalanceTransfer));

            return new LimitedResult<BalanceTransfer>(transfers, numberOfAllElements, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BalanceTransfer[] findTransfersByPairId(final String transferPairId) {
        try (Connection connection = dataSource.getConnection()) {
            QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
            List<BalanceTransfer> balanceTransfers = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceTransfer)
                    .where(qBalanceTransfer.transferPairId.eq(transferPairId))
                    .list(createTransferExpression(qBalanceTransfer));
            return balanceTransfers.toArray(new BalanceTransfer[] {});
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateTransferPairId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }

    private BalanceTransfer lockTransfer(final Connection connection, final long transferId) {
        return transactionHelper.mandatory(new Callback<BalanceTransfer>() {

            @Override
            public BalanceTransfer execute() {
                QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
                List<BalanceTransfer> transfers = new SQLQuery(connection, sqlTemplates)
                        .from(qBalanceTransfer)
                        .where(qBalanceTransfer.transferId.eq(transferId))
                        .limit(1)
                        .forUpdate()
                        .list(createTransferExpression(qBalanceTransfer));
                if (transfers.isEmpty()) {
                    throw new NonExistentTransferException("transfer not found by transferId [" + transferId + "]");
                }
                BalanceTransfer blockedTransfer = transfers.get(0);
                if (!TransferStatus.BLOCKED.equals(blockedTransfer.getTransferStatus())) {
                    throw new InvalidTransferOperationException("transfer status must be blocked instead of ["
                            + blockedTransfer.getTransferStatus() + "]");
                }
                return blockedTransfer;
            }

        });
    }

    @Override
    public void rejectBlockedTransfer(final long transferId) {
        transactionHelper.required(new Callback<Object>() {

            @Override
            public Object execute() {
                try (Connection connection = dataSource.getConnection()) {
                    BalanceTransfer blockedTransfer = lockTransfer(connection, transferId);

                    long creditorAccountId = blockedTransfer.getCreditorAccountId();
                    balanceAccountService.lockAccounts(creditorAccountId);

                    BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
                    if (!creditorAccount.isActive()) {
                        throw new InactiveCreditorException("cannot accomplish the operation on accountId ["
                                + creditorAccountId + "]");
                    }

                    BigDecimal amount = blockedTransfer.getAmount();
                    BigDecimal creditorAvailableBalance = creditorAccount.getAvailableBalance().subtract(amount);
                    BigDecimal creditorBlockedBalance = creditorAccount.getBlockedBalance().add(amount);

                    Calendar now = Calendar.getInstance();

                    QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                    new SQLUpdateClause(connection, sqlTemplates, qBalanceAccount)
                            .where(qBalanceAccount.accountId.eq(creditorAccountId))
                            .set(qBalanceAccount.availableBalance, creditorAvailableBalance.doubleValue())
                            .set(qBalanceAccount.blockedBalance, creditorBlockedBalance.doubleValue())
                            .execute();

                    QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
                    new SQLUpdateClause(connection, sqlTemplates, qBalanceTransfer)
                            .where(qBalanceTransfer.transferId.eq(transferId))
                            .set(qBalanceTransfer.transferStatus, TransferStatus.REJECTED.name())
                            .set(qBalanceTransfer.accomplishedAt, new Timestamp(now.getTimeInMillis()))
                            .set(qBalanceTransfer.lastCreditorAvailableBalance, creditorAvailableBalance.doubleValue())
                            .set(qBalanceTransfer.lastCreditorBlockedBalance, creditorBlockedBalance.doubleValue())
                            .execute();

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

        });
    }

    /**
     * Creates a transfer record and updates the balances of the creditor account.
     * <p>
     * <b>Note:</b> The used accounts must be locked before invoking this method.
     * </p>
     *
     * @param transferPairId
     */
    private long transfer(final Connection connection, final String transferPairId, final String transferCode,
            final long creditorAccountId, final long debtorAccountId, final BigDecimal amount, final String notes,
            final boolean instant, final Calendar timestamp) {

        if (creditorAccountId == debtorAccountId) {
            throw new SameAccountsException("cannot transfer between same accounts, creditorAccountId ["
                    + creditorAccountId + "] and debtorAccountId [" + debtorAccountId + "]");
        }

        BalanceAccount creditorAccount = balanceAccountService.findAccountById(creditorAccountId);
        BalanceAccount debtorAccount = balanceAccountService.findAccountById(debtorAccountId);

        if (!creditorAccount.isActive()) {
            throw new InactiveCreditorException("inactive creditor account, creditorAccountId [" + creditorAccountId
                    + "]");
        } else if (!debtorAccount.isActive()) {
            throw new InactiveDebtorException("inactive debtor account, debtorAccountId [" + debtorAccountId + "]");
        }
        Timestamp accomplishedAtTimestamp = null;
        TransferStatus transferStatus = instant ? TransferStatus.SUCCESSFUL : TransferStatus.BLOCKED;
        BigDecimal lastCreditorAvailableBalance = creditorAccount.getAvailableBalance().add(amount);
        BigDecimal lastCreditorBlockedBalance;
        if (instant) {
            accomplishedAtTimestamp = new Timestamp(timestamp.getTimeInMillis());
            lastCreditorBlockedBalance = creditorAccount.getBlockedBalance();
        } else {
            lastCreditorBlockedBalance = creditorAccount.getBlockedBalance().subtract(amount);
        }
        long resourceId = resourceService.createResource();

        QBalanceTransfer qBalanceTransfer = QBalanceTransfer.balTransfer;
        long transferId = new SQLInsertClause(connection, sqlTemplates, qBalanceTransfer)
                .set(qBalanceTransfer.transferPairId, transferPairId)
                .set(qBalanceTransfer.transferCode, transferCode)
                .set(qBalanceTransfer.creditorAccountId, creditorAccountId)
                .set(qBalanceTransfer.debtorAccountId, debtorAccountId)
                .set(qBalanceTransfer.amount, amount.doubleValue())
                .set(qBalanceTransfer.lastCreditorAvailableBalance, lastCreditorAvailableBalance.doubleValue())
                .set(qBalanceTransfer.lastCreditorBlockedBalance, lastCreditorBlockedBalance.doubleValue())
                .set(qBalanceTransfer.transferStatus, transferStatus.name())
                .set(qBalanceTransfer.createdAt, new Timestamp(timestamp.getTimeInMillis()))
                .set(qBalanceTransfer.accomplishedAt, accomplishedAtTimestamp)
                .set(qBalanceTransfer.notes, notes)
                .set(qBalanceTransfer.resourceId, resourceId)
                .executeWithKey(qBalanceTransfer.transferId);

        QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
        new SQLUpdateClause(connection, sqlTemplates, qBalanceAccount)
                .where(qBalanceAccount.accountId.eq(creditorAccountId))
                .set(qBalanceAccount.availableBalance, lastCreditorAvailableBalance.doubleValue())
                .set(qBalanceAccount.blockedBalance, lastCreditorBlockedBalance.doubleValue())
                .execute();

        return transferId;
    }

}
