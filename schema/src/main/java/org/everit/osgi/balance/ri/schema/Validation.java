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
package org.everit.osgi.balance.ri.schema;

/**
 * Schema validation values.
 */
public interface Validation {

    /**
     * The precision of the balance.
     */
    int BALANCE_PRECISION = 16;

    /**
     * The scale of the balance.
     */
    int BALANCE_SCALE = 4;

    /**
     * The maximum length of the transfer code field.
     */
    int TRANSFER_CODE_MAX_LENGTH = 255;

    /**
     * The maximum length of the notes field.
     */
    int NOTES_MAX_LENGTH = 1023;

    /**
     * Returns the maximum length of the notes field.
     *
     * @return The maximum length of the notes field.
     */
    int getNotesMaxLength();

    /**
     * Returns the maximum length of the transfer code field.
     *
     * @return The maximum length of the transfer code field.
     */
    int getTransferCodeMaxLength();

}
