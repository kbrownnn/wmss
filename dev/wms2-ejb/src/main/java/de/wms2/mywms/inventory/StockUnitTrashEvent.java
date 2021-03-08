/* 
Copyright 2019 Matthias Krane
info@krane.engineer

This file is part of the Warehouse Management System mywms

mywms is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/
package de.wms2.mywms.inventory;

import org.mywms.model.User;

/**
 * This event fired when a stock unit is sent to trash. If a complete UnitLoad
 * is trashed, the UnitLoadTrashEvent is fired, not this one.
 * 
 * @author krane
 *
 */
public class StockUnitTrashEvent {
	private StockUnit stock;
	private String activityCode;
	private User operator;
	private String note;

	public StockUnitTrashEvent(StockUnit stock, String activityCode, User operator, String note) {
		this.stock = stock;
		this.activityCode = activityCode;
		this.operator = operator;
		this.note = note;
	}

	public StockUnit getStock() {
		return stock;
	}

	public String getActivityCode() {
		return activityCode;
	}

	public User getOperator() {
		return operator;
	}

	public String getNote() {
		return note;
	}
}
