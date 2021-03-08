/*
 * Copyright (c) 2010-2013 LinogistiX GmbH
 * 
 * www.linogistix.com
 * 
 * Project: myWMS-LOS
*/
package de.linogistix.mobileserver.processes.controller;

import java.util.Comparator;
import java.util.List;

import javax.ejb.Local;

import de.linogistix.mobileserver.processes.picking.PickingMobilePos;
import de.wms2.mywms.picking.PickingOrder;

@Local
public interface ManageMobile {

	public List<MobileFunction> getFunctions();
	
	public int getMenuPageSize();

	public Comparator<PickingMobilePos> getPickingComparator();
	public String getPickingSelectionText(PickingOrder pickingOrder);
}
