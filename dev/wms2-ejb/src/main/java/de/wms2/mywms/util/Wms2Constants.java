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
package de.wms2.mywms.util;

/**
 * Constants for the wms2 module
 * 
 * @author krane
 *
 */
public class Wms2Constants {
	public final static int FIELDSIZE_DESCRIPTION = 2000;
	public final static int FIELDSIZE_NOTE = 32000;

	/**
	 * The name of an undefined zone. Used in system property KEY_STRATEGY_ZONE_FLOW
	 */
	public final static String UNDEFINED_ZONE_NAME = "NONE";

	/**
	 * Maximal number of layers of carriers
	 */
	public final static int MAX_CARRIER_DEPTH = 10;


}
