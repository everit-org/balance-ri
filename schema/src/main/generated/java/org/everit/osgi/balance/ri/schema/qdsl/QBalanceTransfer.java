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
 * QBalanceTransfer is a Querydsl query type for QBalanceTransfer
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QBalanceTransfer extends com.mysema.query.sql.RelationalPathBase<QBalanceTransfer> {

    private static final long serialVersionUID = -1108814365;

    public static final QBalanceTransfer balTransfer = new QBalanceTransfer("bal_transfer");

    public final DateTimePath<java.sql.Timestamp> accomplishedAt = createDateTime("accomplishedAt", java.sql.Timestamp.class);

    public final NumberPath<Double> amount = createNumber("amount", Double.class);

    public final DateTimePath<java.sql.Timestamp> createdAt = createDateTime("createdAt", java.sql.Timestamp.class);

    public final NumberPath<Long> creditorAccountId = createNumber("creditorAccountId", Long.class);

    public final NumberPath<Long> debtorAccountId = createNumber("debtorAccountId", Long.class);

    public final NumberPath<Double> lastCreditorAvailableBalance = createNumber("lastCreditorAvailableBalance", Double.class);

    public final NumberPath<Double> lastCreditorBlockedBalance = createNumber("lastCreditorBlockedBalance", Double.class);

    public final StringPath notes = createString("notes");

    public final NumberPath<Long> resourceId = createNumber("resourceId", Long.class);

    public final StringPath transferCode = createString("transferCode");

    public final NumberPath<Long> transferId = createNumber("transferId", Long.class);

    public final StringPath transferPairId = createString("transferPairId");

    public final StringPath transferStatus = createString("transferStatus");

    public final com.mysema.query.sql.PrimaryKey<QBalanceTransfer> balTransferPk = createPrimaryKey(transferId);

    public final com.mysema.query.sql.ForeignKey<org.everit.osgi.resource.schema.qdsl.QResource> transferResourceFk = createForeignKey(resourceId, "resource_id");

    public final com.mysema.query.sql.ForeignKey<QBalanceAccount> transferDebtorFk = createForeignKey(debtorAccountId, "account_id");

    public final com.mysema.query.sql.ForeignKey<QBalanceAccount> transferCreditorFk = createForeignKey(creditorAccountId, "account_id");

    public QBalanceTransfer(String variable) {
        super(QBalanceTransfer.class, forVariable(variable), null, "bal_transfer");
        addMetadata();
    }

    public QBalanceTransfer(String variable, String schema, String table) {
        super(QBalanceTransfer.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QBalanceTransfer(Path<? extends QBalanceTransfer> path) {
        super(path.getType(), path.getMetadata(), null, "bal_transfer");
        addMetadata();
    }

    public QBalanceTransfer(PathMetadata<?> metadata) {
        super(QBalanceTransfer.class, metadata, null, "bal_transfer");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(accomplishedAt, ColumnMetadata.named("accomplished_at").ofType(93).withSize(23).withDigits(10));
        addMetadata(amount, ColumnMetadata.named("amount").ofType(3).withSize(16).withDigits(4).notNull());
        addMetadata(createdAt, ColumnMetadata.named("created_at").ofType(93).withSize(23).withDigits(10).notNull());
        addMetadata(creditorAccountId, ColumnMetadata.named("creditor_account_id").ofType(-5).withSize(19).notNull());
        addMetadata(debtorAccountId, ColumnMetadata.named("debtor_account_id").ofType(-5).withSize(19).notNull());
        addMetadata(lastCreditorAvailableBalance, ColumnMetadata.named("last_creditor_available_balance").ofType(3).withSize(16).withDigits(4).notNull());
        addMetadata(lastCreditorBlockedBalance, ColumnMetadata.named("last_creditor_blocked_balance").ofType(3).withSize(16).withDigits(4).notNull());
        addMetadata(notes, ColumnMetadata.named("notes").ofType(12).withSize(1023).notNull());
        addMetadata(resourceId, ColumnMetadata.named("resource_id").ofType(-5).withSize(19).notNull());
        addMetadata(transferCode, ColumnMetadata.named("transfer_code").ofType(12).withSize(255).notNull());
        addMetadata(transferId, ColumnMetadata.named("transfer_id").ofType(-5).withSize(19).notNull());
        addMetadata(transferPairId, ColumnMetadata.named("transfer_pair_id").ofType(12).withSize(32).notNull());
        addMetadata(transferStatus, ColumnMetadata.named("transfer_status").ofType(12).withSize(31).notNull());
    }

}

