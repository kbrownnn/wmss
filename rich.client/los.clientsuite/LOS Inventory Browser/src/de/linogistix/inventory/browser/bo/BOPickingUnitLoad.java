/*
 * Copyright (c) 2009-2012 LinogistiX GmbH
 *
 *  www.linogistix.com
 *
 *  Project myWMS-LOS
 */

package de.linogistix.inventory.browser.bo;

import de.linogistix.common.bobrowser.bo.BO;
import de.linogistix.common.bobrowser.query.gui.component.AutoCompletionQueryFilterProvider;
import de.linogistix.common.bobrowser.query.gui.component.BOQueryComponentProvider;
import de.linogistix.common.bobrowser.query.gui.component.DefaultBOQueryComponentProvider;
import de.linogistix.common.bobrowser.query.gui.component.TemplateQueryWizardProvider;
import de.linogistix.common.services.J2EEServiceLocator;
import de.linogistix.common.util.ExceptionAnnotator;
import de.linogistix.inventory.browser.masternode.BOPickingUnitLoadMasterNode;
import de.linogistix.inventory.res.InventoryBundleResolver;
import de.linogistix.los.crud.BusinessObjectCRUDRemote;
import de.linogistix.los.inventory.crud.LOSPickingUnitLoadCRUDRemote;
import de.linogistix.los.inventory.query.LOSPickingUnitLoadQueryRemote;
import de.linogistix.los.model.State;
import de.linogistix.los.query.BusinessObjectQueryRemote;
import de.linogistix.los.query.QueryDetail;
import de.linogistix.los.query.TemplateQuery;
import de.wms2.mywms.picking.Packet;
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

/**
 *
 * @author krane
 */
public class BOPickingUnitLoad extends BO {
    @Override
    public String[] getAllowedRoles() {
        return new String[] {Role.ADMIN_STR,Role.OPERATOR_STR,Role.FOREMAN_STR,Role.INVENTORY_STR,Role.CLEARING_STR};
    }
    
    @Override
    public String[] getAllowedRolesCRUD() {
        return new String[] {Role.ADMIN_STR};
    }
 
    protected String initName() {
        return "PickingUnitLoads";
    }
  
    protected String initIconBaseWithExtension() {
        return "de/linogistix/bobrowser/res/icon/OrderRequestPosition.png";
    }
  
  
  protected BusinessObjectQueryRemote initQueryService() {
    
    BusinessObjectQueryRemote ret = null;
    
    try{
      J2EEServiceLocator loc = (J2EEServiceLocator)Lookup.getDefault().lookup(J2EEServiceLocator.class);
      ret = loc.getStateless(LOSPickingUnitLoadQueryRemote.class);
      
    } catch (Throwable t){
      //
    }
    return ret;
  }
  
  
  protected BasicEntity initEntityTemplate() {
      Packet c;
    
    c = new Packet();
    
    return c;
    
  }
  
  protected BusinessObjectCRUDRemote initCRUDService(){
    BusinessObjectCRUDRemote ret = null;
    
    try{
      J2EEServiceLocator loc = (J2EEServiceLocator)Lookup.getDefault().lookup(J2EEServiceLocator.class);
      ret = loc.getStateless(LOSPickingUnitLoadCRUDRemote.class);
      
    } catch (Throwable t){
      //ExceptionAnnotator.annotate(t);
    }
    return ret;
  }
  
  @Override
  protected Class initBundleResolver() {
    return InventoryBundleResolver.class;
  }
  
  @Override
  protected String[] initIdentifiableProperties() {
    return new String[]{"unitLoad.labelId"};
  }

    @Override
    protected Property[] initBoMasterNodeProperties() {
        return BOPickingUnitLoadMasterNode.boMasterNodeProperties();
    }

    @Override
    protected Class<? extends Node> initBoMasterNodeType() {
        return BOPickingUnitLoadMasterNode.class;
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
            entryList.add(State.RAW);
            entryList.add(State.STARTED);
            entryList.add(State.PICKED);
            entryList.add(OrderState.SHIPPING);
            entryList.add(State.FINISHED);

            return entryList;
        }

        return super.getValueList(fieldName);
    }

}
