/**
 * This file is part of org.everit.osgi.balance.ri.schema.
 *
 * org.everit.osgi.balance.ri.schema is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * org.everit.osgi.balance.ri.schema is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with org.everit.osgi.balance.ri.schema.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.balance.ri.schema.qdsl;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QBalanceAccount is a Querydsl query type for QBalanceAccount
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QBalanceAccount extends com.mysema.query.sql.RelationalPathBase<QBalanceAccount> {

    private static final long serialVersionUID = -146027211;

    public static final QBalanceAccount balAccount = new QBalanceAccount("bal_account");

    public final NumberPath<Long> accountId = createNumber("accountId", Long.class);

    public final BooleanPath active = createBoolean("active");

    public final NumberPath<Double> availableBalance = createNumber("availableBalance", Double.class);

    public final NumberPath<Double> blockedBalance = createNumber("blockedBalance", Double.class);

    public final NumberPath<Long> ownerResourceId = createNumber("ownerResourceId", Long.class);

    public final NumberPath<Long> resourceId = createNumber("resourceId", Long.class);

    public final com.mysema.query.sql.PrimaryKey<QBalanceAccount> balAccountPk = createPrimaryKey(accountId);

    public final com.mysema.query.sql.ForeignKey<org.everit.osgi.resource.schema.qdsl.QResource> accountOwnerResourceFk = createForeignKey(ownerResourceId, "resource_id");

    public final com.mysema.query.sql.ForeignKey<org.everit.osgi.resource.schema.qdsl.QResource> accountResourceFk = createForeignKey(resourceId, "resource_id");

    public final com.mysema.query.sql.ForeignKey<QBalanceTransfer> _transferDebtorFk = createInvForeignKey(accountId, "debtor_account_id");

    public final com.mysema.query.sql.ForeignKey<QBalanceTransfer> _transferCreditorFk = createInvForeignKey(accountId, "creditor_account_id");

    public QBalanceAccount(String variable) {
        super(QBalanceAccount.class, forVariable(variable), null, "bal_account");
        addMetadata();
    }

    public QBalanceAccount(String variable, String schema, String table) {
        super(QBalanceAccount.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QBalanceAccount(Path<? extends QBalanceAccount> path) {
        super(path.getType(), path.getMetadata(), null, "bal_account");
        addMetadata();
    }

    public QBalanceAccount(PathMetadata<?> metadata) {
        super(QBalanceAccount.class, metadata, null, "bal_account");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(accountId, ColumnMetadata.named("account_id").ofType(-5).withSize(19).notNull());
        addMetadata(active, ColumnMetadata.named("active").ofType(16).withSize(1).notNull());
        addMetadata(availableBalance, ColumnMetadata.named("available_balance").ofType(3).withSize(16).withDigits(4).notNull());
        addMetadata(blockedBalance, ColumnMetadata.named("blocked_balance").ofType(3).withSize(16).withDigits(4).notNull());
        addMetadata(ownerResourceId, ColumnMetadata.named("owner_resource_id").ofType(-5).withSize(19).notNull());
        addMetadata(resourceId, ColumnMetadata.named("resource_id").ofType(-5).withSize(19).notNull());
    }

}

