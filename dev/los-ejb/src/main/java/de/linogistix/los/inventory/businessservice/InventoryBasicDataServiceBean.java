/*
 * Copyright (c) 2012-2013 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.businessservice;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;
import org.mywms.model.Client;

import de.linogistix.los.inventory.model.LOSInventoryPropertyKey;
import de.linogistix.los.inventory.res.InventoryBundleResolver;
import de.linogistix.los.inventory.service.ItemUnitService;
import de.linogistix.los.model.LOSCommonPropertyKey;
import de.linogistix.los.util.entityservice.LOSSystemPropertyService;
import de.wms2.mywms.client.ClientBusiness;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.location.StorageLocationEntityService;
import de.wms2.mywms.product.ItemUnit;
import de.wms2.mywms.property.SystemProperty;
import de.wms2.mywms.strategy.OrderStrategyEntityService;
import de.wms2.mywms.strategy.StorageStrategyEntityService;


/**
 * @author krane
 *
 */
@Stateless
public class InventoryBasicDataServiceBean implements InventoryBasicDataService {

	private static final Logger log = Logger.getLogger(InventoryBasicDataServiceBean.class);

	@Inject
	private ClientBusiness clientService;
	@Inject
	private OrderStrategyEntityService orderStrategyService;
	@EJB
	private LOSSystemPropertyService propertyService;
	@EJB
	private ItemUnitService unitService;
	@Inject
	private StorageStrategyEntityService storageStrategyService;
	@Inject
	private StorageLocationEntityService locationService;

	public void createBasicData(Locale locale) throws FacadeException {

		log.info("Create Inventory Basic Data...");

		Client sys = clientService.getSystemClient();

		log.info("Create Strategies...");
		
		orderStrategyService.getDefault(sys);
		orderStrategyService.getExtinguish(sys);
		
		storageStrategyService.getDefault();
		
		log.info("Create Properties...");
		propertyService.createSystemProperty(sys, null, LOSInventoryPropertyKey.PRINT_GOODS_RECEIPT_LABEL, "false", LOSCommonPropertyKey.PROPERTY_GROUP_SERVER, resolve("PropertyDescPRINT_GOODS_RECEIPT_LABEL", locale), false);
		propertyService.createSystemProperty(sys, null, LOSInventoryPropertyKey.GOODS_IN_DEFAULT_LOCK, "0", LOSCommonPropertyKey.PROPERTY_GROUP_SERVER, resolve("PropertyDescGOODS_IN_DEFAULT_LOCK", locale), false);
		SystemProperty goodsInLocationProperty = propertyService.createSystemProperty(sys, null, LOSInventoryPropertyKey.DEFAULT_GOODS_RECEIPT_LOCATION_NAME, null, LOSCommonPropertyKey.PROPERTY_GROUP_CLIENT, resolve("PropertyDescDEFAULT_GOODS_RECEIPT_LOCATION_NAME", locale), false);
		propertyService.createSystemProperty(sys, null, LOSInventoryPropertyKey.GOODS_RECEIPT_PRINTER, null, LOSCommonPropertyKey.PROPERTY_GROUP_CLIENT, resolve("PropertyDescGOODS_RECEIPT_PRINTER", locale), false);

		List<StorageLocation> goodsInLocations = locationService.getForGoodsIn(null);
		if (goodsInLocations.size() == 1 && StringUtils.isBlank(goodsInLocationProperty.getPropertyValue())) {
			goodsInLocationProperty.setPropertyValue(goodsInLocations.get(0).getName());
		}

		ItemUnit pce = unitService.getDefault();
		pce.setName( resolve("BasicDataItemUnitPcs", locale) );
		
		log.info("Create Inventory Basic Data. done.");
	}
	
	
	private final String resolve( String key, Locale locale ) {
        if (key == null) {
            return "";
        }
        
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("de.linogistix.los.inventory.res.Bundle", locale, InventoryBundleResolver.class.getClassLoader());
            String s = bundle.getString(key);
            return s;
        }
        catch (MissingResourceException ex) {
        	log.error("Exception: "+ex.getMessage());
            return key;
        }
        catch (IllegalFormatException ife){
        	log.error("Exception: "+ife.getMessage());
        	return key;
        }
    }
}
