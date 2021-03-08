/* 
Copyright 2019 Matthias Krane

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
package de.linogistix.los.inventory.query;

import javax.ejb.Stateless;

import de.linogistix.los.inventory.query.dto.AddressTO;
import de.linogistix.los.query.BusinessObjectQueryBean;
import de.wms2.mywms.address.Address;

/**
 * 
 * @author krane
 */
@Stateless
public class AddressQueryBean extends BusinessObjectQueryBean<Address> implements AddressQueryRemote {
	@Override
	public Class<AddressTO> getBODTOClass() {
		return AddressTO.class;
	}

	@Override
	protected String[] getBODTOConstructorProps() {
		return new String[] { "id", "version", "firstName", "lastName", "city" };
	}
}
