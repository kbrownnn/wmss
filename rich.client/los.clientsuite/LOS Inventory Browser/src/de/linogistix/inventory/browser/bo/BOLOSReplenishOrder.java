/*
 * BONodeClient.java
 *
 * Created on 1. Dezember 2006, 01:17
 *
 * Copyright (c) 2006 LinogistiX GmbH. All rights reserved.
 *
 *<a href="http://www.linogistix.com/">browse for licence information</a>
 *
 */

package de.linogistix.inventory.browser.bo;

import de.linogistix.common.bobrowser.bo.BO;
import de.linogistix.common.bobrowser.query.gui.component.AutoCompletionQueryFilterProvider;
import de.linogistix.common.bobrowser.query.gui.component.BOQueryComponentProvider;
import de.linogistix.common.bobrowser.query.gui.component.DefaultBOQueryComponentProvider;
import de.linogistix.common.bobrowser.query.gui.component.TemplateQueryWizardProvider;
import de.linogistix.common.services.J2EEServiceLocator;
import de.linogistix.common.util.ExceptionAnnotator;
import de.linogistix.inventory.browser.masternode.BOLOSReplenishOrderMasterNode;
import de.linogistix.inventory.res.InventoryBundleResolver;
import de.linogistix.los.crud.BusinessObjectCRUDRemote;
import de.linogistix.los.inventory.crud.LOSReplenishOrderCRUDRemote;
import de.linogistix.los.inventory.query.LOSReplenishOrderQueryRemote;
import de.linogistix.los.model.Prio;
import de.linogistix.los.query.BusinessObjectQueryRemote;
import de.linogistix.los.query.QueryDetail;
import de.linogistix.los.query.TemplateQuery;
import de.wms2.mywms.replenish.ReplenishOrder;
import de.wms2.mywms.strategy.OrderState;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.mywms.globals.Role;
import org.mywms.model.BasicEntity;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author <a href="http://community.mywms.de/developer.jsp">Andreas Trautmann</a>
 */
public class BOLOSReplenishOrder extends BO {
    @Override
    public String[] getAllowedRoles() {
        return new String[] {Role.ADMIN_STR,Role.OPERATOR_STR,Role.FOREMAN_STR,Role.INVENTORY_STR,Role.CLEARING_STR};
    }
    
    @Override
    public String[] getAllowedRolesCRUD() {
        return new String[] {Role.ADMIN_STR};
    }
    
   protected  String initName() {
    return "ReplenishOrders";
  }
  
  protected String initIconBaseWithExtension() {
    return "de/linogistix/bobrowser/res/icon/ReplenishRequest.png";
  }
  
  
  protected BusinessObjectQueryRemote initQueryService() {
    
    BusinessObjectQueryRemote ret = null;
    
    try{
      J2EEServiceLocator loc = (J2EEServiceLocator)Lookup.getDefault().lookup(J2EEServiceLocator.class);
      ret = (LOSReplenishOrderQueryRemote)loc.getStateless(LOSReplenishOrderQueryRemote.class);
      
    } catch (Throwable t){
      //
    }
    return ret;
  }
  
  
  protected BasicEntity initEntityTemplate() {
      ReplenishOrder c;
    
    c = new ReplenishOrder();
    
    return c;
    
  }
  
  protected BusinessObjectCRUDRemote initCRUDService(){
    LOSReplenishOrderCRUDRemote ret = null;
    
    try{
      J2EEServiceLocator loc = (J2EEServiceLocator)Lookup.getDefault().lookup(J2EEServiceLocator.class);
      ret = (LOSReplenishOrderCRUDRemote)loc.getStateless(LOSReplenishOrderCRUDRemote.class);
      
    } catch (Throwable t){
      //ExceptionAnnotator.annotate(t);
    }
    return ret;
  }
  
  protected Class initBundleResolver() {
    return InventoryBundleResolver.class;
  }
  
   protected String[] initIdentifiableProperties() {
    return new String[]{"number"};
  }
  
   @Override
    protected List<SystemAction> initActions() {
        List<SystemAction> actions = new ArrayList<SystemAction>(); 
        actions = super.initActions();

        
        return actions;
    }
   

    @Override
    protected Property[] initBoMasterNodeProperties() {
        return BOLOSReplenishOrderMasterNode.boMasterNodeProperties();
    }

    @Override
    protected Class<? extends Node> initBoMasterNodeType() {
        return BOLOSReplenishOrderMasterNode.class;
    }

    @Override
    public List<BOQueryComponentProvider> initQueryComponentProviders() {
        try {
            List<BOQueryComponentProvider> retDesired = new ArrayList();

            AutoCompletionQueryFilterProvider quickSearch = new AutoCompletionQueryFilterProvider(getQueryService());
            quickSearch.addFilter( 1, NbBundle.getMessage(InventoryBundleResolver.class, "filter")+":" );
            quickSearch.addFilterValue(1, NbBundle.getMessage(InventoryBundleResolver.class, "filter.all"), "ALL");
            quickSearch.addFilterValue(1, NbBundle.getMessage(InventoryBundleResolver.class, "filter.open"), "OPEN");
            quickSearch.setFilterSelected(1, 1);
            retDesired.add( quickSearch );

            Method m;
            m = getQueryService().getClass().getDeclaredMethod("queryAllHandles", new Class[]{QueryDetail.class});
            retDesired.add(new DefaultBOQueryComponentProvider(getQueryService(), m));

            m = getQueryService().getClass().getDeclaredMethod("queryByTemplateHandles", new Class[]{QueryDetail.class, TemplateQuery.class});
            retDesired.add(new TemplateQueryWizardProvider(getQueryService(), m));

            return retDesired;
        } catch (Throwable ex) {
           ExceptionAnnotator.annotate(ex);
           return new ArrayList();
        }
    }

    @Override
    public List<Object> getValueList(String fieldName) {
        if( "state".equals(fieldName) ) {
            List<Object> entryList = new ArrayList<Object>();
            entryList.add(OrderState.PROCESSABLE);
            entryList.add(OrderState.STARTED);
            entryList.add(OrderState.PICKED);
            entryList.add(OrderState.FINISHED);
            entryList.add(OrderState.DELETABLE);

            return entryList;
        }
        if( "prio".equals(fieldName) ) {
            List<Object> entryList = new ArrayList<Object>();
            entryList.add(Prio.LOW);
            entryList.add(Prio.NORMAL);
            entryList.add(Prio.HIGH);

            return entryList;
        }
        return super.getValueList(fieldName);
    }
}
