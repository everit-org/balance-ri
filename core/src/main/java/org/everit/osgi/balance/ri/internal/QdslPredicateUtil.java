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
package org.everit.osgi.balance.ri.internal;

import java.sql.Timestamp;
import java.util.Calendar;

import org.everit.commons.selection.sql.TimestampRange;
import org.everit.commons.selection.util.CalendarRange;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

public final class QdslPredicateUtil {

    public static Predicate between(final DateTimePath<Timestamp> dateTimePath, final CalendarRange range) {
        return QdslPredicateUtil.between(dateTimePath, QdslPredicateUtil.convert(range));
    }

    public static Predicate between(final DateTimePath<Timestamp> dateTimePath, final TimestampRange range) {
        if (range == null) {
            return null;
        }
        BooleanExpression lowerExpr = null;
        Timestamp lowerBound = range.getLowerBound();
        if (lowerBound != null) {
            if (range.isLowerInclusive()) {
                lowerExpr = dateTimePath.goe(lowerBound);
            } else {
                lowerExpr = dateTimePath.after(lowerBound);
            }
        }
        BooleanExpression higherExpr = null;
        Timestamp higherBound = range.getHigherBound();
        if (higherBound != null) {
            if (range.isHigherInclusive()) {
                higherExpr = dateTimePath.loe(higherBound);
            } else {
                higherExpr = dateTimePath.before(higherBound);
            }
        }
        return BooleanExpression.allOf(lowerExpr, higherExpr);
    }

    private static TimestampRange convert(final CalendarRange range) {
        if (range == null) {
            return null;
        }
        Calendar lowerBound = range.getLowerBound();
        Calendar higherBound = range.getHigherBound();
        return new TimestampRange(
                lowerBound == null ? null : new Timestamp(lowerBound.getTimeInMillis()),
                higherBound == null ? null : new Timestamp(higherBound.getTimeInMillis()),
                range.isLowerInclusive(),
                range.isHigherInclusive());
    }

    public static <T extends Number & Comparable<T>> Predicate eq(final NumberPath<T> numberPath, final T number) {
        return number == null ? null : numberPath.eq(number);
    }

    public static Predicate eq(final StringPath stringPath, final String string) {
        return string == null ? null : stringPath.eq(string);
    }

    private QdslPredicateUtil() {
    }

}
