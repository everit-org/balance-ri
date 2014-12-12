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

/**
 * Constants of the {@link BalanceAccountComponent}.
 */
public final class BalanceAccountConstants {

    /**
     * The component name of the {@link BalanceAccountComponent}.
     */
    public static final String COMPONENT_NAME = "org.everit.osgi.balance.ri.BalanceAccount";

    /**
     * The property name of the OSGi filter expression defining which
     * {@link org.everit.osgi.resource.api.ResourceService} should be used by {@link BalanceAccountComponent}.
     */
    public static final String PROP_RESOURCE_SERVICE_TARGET = "resourceService.target";

    public static final String PROP_TRANSACTION_HELPER = "transactionHelper.target";

    public static final String PROP_DATA_SOURCE = "dataSource.target";

    public static final String PROP_SQL_TEMPLATES = "sqlTemplates.target";

    private BalanceAccountConstants() {
    }

}
