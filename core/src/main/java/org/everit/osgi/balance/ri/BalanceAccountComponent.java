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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.ArrayUtils;
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
import org.everit.osgi.balance.api.exception.AccountLockException;
import org.everit.osgi.balance.ri.schema.qdsl.QBalanceAccount;
import org.everit.osgi.resource.api.ResourceService;
import org.everit.osgi.transaction.helper.api.Callback;
import org.everit.osgi.transaction.helper.api.TransactionHelper;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.ConstructorExpression;

/**
 * The reference implementation of the {@link BalanceAccountService}.
 */
@Component(name = BalanceAccountConstants.COMPONENT_NAME, metatype = true,
        configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = BalanceAccountConstants.PROP_TRANSACTION_HELPER),
        @Property(name = BalanceAccountConstants.PROP_DATA_SOURCE),
        @Property(name = BalanceAccountConstants.PROP_SQL_TEMPLATES),
        @Property(name = BalanceAccountConstants.PROP_RESOURCE_SERVICE_TARGET)
})
@Service
public class BalanceAccountComponent implements BalanceAccountService {

    @Reference
    private TransactionHelper transactionHelper;

    @Reference
    private DataSource dataSource;

    @Reference
    private SQLTemplates sqlTemplates;

    @Reference
    private ResourceService resourceService;

    @Override
    public long activateAccount(final long accountId) {
        return setAccountActive(accountId, true);
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

    private long countAccountsByOwnerResourceId(final Connection connection, final QBalanceAccount qBalanceAccount,
            final Long ownerResourceId) {
        SQLQuery sqlQuery = new SQLQuery(connection, sqlTemplates)
                .from(qBalanceAccount);
        if (ownerResourceId != null) {
            sqlQuery.where(qBalanceAccount.ownerResourceId.eq(ownerResourceId));
        }
        long count = sqlQuery.count();
        return count;
    }

    @Override
    public long createAccount(final Long ownerResourceId, final boolean active) {
        Long rval = transactionHelper.required(new Callback<Long>() {

            @Override
            public Long execute() {

                long usedOwnerResourceId;
                if (ownerResourceId == null) {
                    usedOwnerResourceId = resourceService.createResource();
                } else {
                    usedOwnerResourceId = ownerResourceId.longValue();
                }

                long resourceId = resourceService.createResource();

                QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                try (Connection connection = dataSource.getConnection()) {
                    long accountId = new SQLInsertClause(connection, sqlTemplates, qBalanceAccount)
                            .set(qBalanceAccount.ownerResourceId, usedOwnerResourceId)
                            .set(qBalanceAccount.resourceId, resourceId)
                            .set(qBalanceAccount.active, true)
                            .executeWithKey(qBalanceAccount.accountId);
                    return accountId;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        });
        return rval;
    }

    private ConstructorExpression<BalanceAccount> createAccountExpression(final QBalanceAccount qBalanceAccount) {
        return ConstructorExpression.create(BalanceAccount.class,
                qBalanceAccount.accountId,
                qBalanceAccount.active,
                qBalanceAccount.availableBalance,
                qBalanceAccount.blockedBalance,
                qBalanceAccount.ownerResourceId,
                qBalanceAccount.resourceId);
    }

    @Override
    public long deactivateAccount(final long accountId) {
        return setAccountActive(accountId, false);
    }

    @Override
    public BalanceAccount findAccountById(final long accountId) {
        QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
        try (Connection connection = dataSource.getConnection()) {
            List<BalanceAccount> balanceAccounts = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceAccount)
                    .where(qBalanceAccount.accountId.eq(accountId))
                    .limit(1)
                    .list(createAccountExpression(qBalanceAccount));
            if (balanceAccounts.isEmpty()) {
                return null;
            } else {
                return balanceAccounts.get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BalanceAccount findAccountByResourceId(final long resourceId) {
        QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
        List<BalanceAccount> balanceAccounts;
        try (Connection connection = dataSource.getConnection()) {
            balanceAccounts = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceAccount)
                    .where(qBalanceAccount.resourceId.eq(resourceId))
                    .limit(1)
                    .list(createAccountExpression(qBalanceAccount));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (balanceAccounts.isEmpty()) {
            return null;
        }
        return balanceAccounts.get(0);
    }

    @Override
    public LimitedResult<BalanceAccount> findAccountsByOwnerResourceId(final Long ownerResourceId, final Limit limit) {
        if (limit == null) {
            throw new IllegalArgumentException("limit cannot be null");
        }
        try (Connection connection = dataSource.getConnection()) {

            QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;

            long numberOfAllElements = countAccountsByOwnerResourceId(connection, qBalanceAccount, ownerResourceId);
            if (numberOfAllElements == 0) {
                return new LimitedResult<>(new ArrayList<BalanceAccount>(), numberOfAllElements, limit);
            }

            SQLQuery sqlQuery = new SQLQuery(connection, sqlTemplates)
                    .from(qBalanceAccount);
            if (ownerResourceId != null) {
                sqlQuery.where(qBalanceAccount.ownerResourceId.eq(ownerResourceId));
            }
            List<BalanceAccount> elements = sqlQuery
                    .offset(limit.getFirstResult())
                    .limit(limit.getMaxResults())
                    .list(createAccountExpression(qBalanceAccount));
            return new LimitedResult<>(elements, numberOfAllElements, limit);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long[] lockAccounts(final long... accountIds) {
        if (accountIds == null) {
            throw new IllegalArgumentException("accountIds cannot be null");
        }
        if (accountIds.length < 1) {
            throw new IllegalArgumentException("at least one accountId must be provided");
        }
        final Long[] accountIdsToLock = ArrayUtils.toObject(accountIds);

        List<Long> lockedAccountIds = transactionHelper.mandatory(new Callback<List<Long>>() {

            @Override
            public List<Long> execute() {
                QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                try (Connection connection = dataSource.getConnection()) {
                    return new SQLQuery(connection, sqlTemplates)
                            .from(qBalanceAccount)
                            .where(qBalanceAccount.accountId.in(accountIdsToLock))
                            .forUpdate()
                            .list(qBalanceAccount.accountId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        });
        for (long accountId : accountIds) {
            if (!lockedAccountIds.contains(accountId)) {
                throw new AccountLockException("failed to lock accountId [" + accountId + "]");
            }
        }

        return ArrayUtils.toPrimitive(lockedAccountIds.toArray(new Long[] {}));
    }

    private long setAccountActive(final long accountId, final boolean active) {
        Long rval = transactionHelper.required(new Callback<Long>() {

            @Override
            public Long execute() {
                QBalanceAccount qBalanceAccount = QBalanceAccount.balAccount;
                try (Connection connection = dataSource.getConnection()) {
                    long count = new SQLUpdateClause(connection, sqlTemplates, qBalanceAccount)
                            .where(qBalanceAccount.accountId.eq(accountId))
                            .set(qBalanceAccount.active, active)
                            .execute();
                    return count;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return rval;
    }
}
