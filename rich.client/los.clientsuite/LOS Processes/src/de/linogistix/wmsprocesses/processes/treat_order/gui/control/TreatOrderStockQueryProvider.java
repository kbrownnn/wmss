/*
 * Copyright (c) 2006 - 2010 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.wmsprocesses.processes.treat_order.gui.control;

import de.linogistix.common.bobrowser.query.BOQueryNode;
import de.linogistix.common.bobrowser.query.gui.component.BOQueryComponentProvider;
import de.linogistix.common.bobrowser.query.gui.component.DockingMode;
import de.linogistix.common.bobrowser.query.gui.component.ProviderChangeEventListener;
import de.linogistix.los.inventory.query.StockUnitQueryRemote;
import de.linogistix.los.query.BODTO;
import de.linogistix.los.query.BusinessObjectQueryRemote;
import de.linogistix.los.query.QueryDetail;
import de.linogistix.wmsprocesses.processes.treat_order.gui.model.TreatOrderStockSelectionModel;
import de.wms2.mywms.product.ItemData;
import java.awt.FlowLayout;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openide.WizardDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author trautm
 */
public class TreatOrderStockQueryProvider implements BOQueryComponentProvider {

    private static final Logger log = Logger.getLogger(TreatOrderStockQueryProvider.class.getName());
    
    Method m;
    StockUnitQueryRemote queryRemote;
    
    private ProviderChangeEventListener providerChangeEventListener;
    
    private BOQueryNode bOQueryNode;
    
    private TreatOrderStockSelectionModel myModel;
    
    private JPanel queryPanel;
    
    public TreatOrderStockQueryProvider(StockUnitQueryRemote queryRemote, 
                                        TreatOrderStockSelectionModel model) 
    {    
        queryPanel = new JPanel(new FlowLayout());
        
        myModel = model;
        
        this.queryRemote = queryRemote;
        try {
            
            this.m = this.queryRemote.getClass().getDeclaredMethod("queryByDefault", new Class[]{BODTO.class, String.class,BODTO.class,BODTO.class,QueryDetail.class});
            
        } catch (Throwable ex) {
            log.log(Level.SEVERE,ex.getMessage(),ex);
            throw new RuntimeException();
        }
    }

    public BusinessObjectQueryRemote getQueryRemote() {
        return queryRemote;
    }

    public Method getMethod() {
        return this.m;
    }

    public DockingMode getDockingMode() {
//        return DockingMode.INLPLACE;
        // Or with own textfield in QueryPanel???
        return DockingMode.QUERYPANEL;
    }

    public JComponent createComponent() {
        queryPanel.invalidate();

        return this.queryPanel;
    }

    public WizardDescriptor createWizard() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object[] getQueryMethodParameters(QueryDetail detail, String queryStr) {
        
        return new Object[]{null, null, null, null,detail};
                          
    }

    @Override
    public String toString() {
        String s;
        
        s= NbBundle.getMessage(de.linogistix.wmsprocesses.res.WMSProcessesBundleResolver.class, "defaultSearch");
        return s;
    }

    public BOQueryNode getBOQueryNode() {
        return bOQueryNode;
    }

    public void setBOQueryNode(BOQueryNode bOQueryNode) {
        this.bOQueryNode = bOQueryNode;
    }

    public ProviderChangeEventListener getProviderChangeEventListener() {
        return providerChangeEventListener;
    }

    public void setProviderChangeEventListener(ProviderChangeEventListener providerChangeEventListener) {
        this.providerChangeEventListener = providerChangeEventListener;
    }
    
    public Class[] getQueryMethodParameterTypes() {
        return this.m.getParameterTypes();
    }
    
    public void setItemDataTO(BODTO<ItemData> item){
        
    }

    public void clear(){
    }
    
}
