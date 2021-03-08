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
package de.wms2.mywms.location;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.wms2.mywms.entity.EntityValidator;
import de.wms2.mywms.entity.GenericEntityService;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.util.Wms2BundleResolver;

/**
 * Validation service for Area
 * 
 * @author krane
 *
 */
public class AreaValidator implements EntityValidator<Area> {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Inject
	private GenericEntityService entitySerivce;

	@Override
	public void validateCreate(Area entity) throws BusinessException {
		validate(entity);
	}

	@Override
	public void validateUpdate(Area entityOld, Area entityNew) throws BusinessException {
		validate(entityNew);
	}

	public void validate(Area entity) throws BusinessException {
		String logStr = "validate ";

		if (StringUtils.isEmpty(entity.getName())) {
			logger.log(Level.INFO, logStr + "missing name. entity=" + entity);
			throw new BusinessException(Wms2BundleResolver.class, "Validator.missingName");
		}

		if (StringUtils.contains(entity.getUsages(), AreaUsages.TRANSFER)) {
			if (entity.getUsages().contains(AreaUsages.PACKING) || entity.getUsages().contains(AreaUsages.PICKING)
					|| entity.getUsages().contains(AreaUsages.STORAGE)) {
				logger.log(Level.INFO, logStr
						+ "invalid usage options. TRANSFER cannot be combined with one of PICKING, PICKING, STORAGE. entity="
						+ entity + ", usages=" + entity.getUsages());
				throw new BusinessException(Wms2BundleResolver.class, "Validator.invalidTransferUsage");
			}
		}

		if (entitySerivce.exists(Area.class, "name", entity.getName(), entity.getId())) {
			logger.log(Level.INFO, logStr + "not unique. name=" + entity.getName());
			throw new BusinessException(Wms2BundleResolver.class, "Validator.notUnique");
		}

	}

	@Override
	public void validateDelete(Area entity) throws BusinessException {
		String logStr = "validateDelete ";

		if (entitySerivce.exists(StorageLocation.class, "area", entity)) {
			logger.log(Level.INFO, logStr + "Existing reference to StorageLocation. entity=" + entity);
			throw new BusinessException(Wms2BundleResolver.class, "Validator.usedByStorageLocation");
		}
	}
}
