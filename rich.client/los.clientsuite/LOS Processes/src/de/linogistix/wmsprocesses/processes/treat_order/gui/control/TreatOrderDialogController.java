/*
 * Copyright (c) 2006 - 2010 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.wmsprocesses.processes.treat_order.gui.control;

import de.linogistix.common.gui.component.controls.BOAutoFilteringComboBox;
import de.linogistix.common.gui.component.view.LOSListViewSelectionListener;
import de.linogistix.common.gui.object.IconType;
import de.linogistix.common.services.J2EEServiceLocator;
import de.linogistix.common.util.ExceptionAnnotator;
import de.linogistix.common.util.GraphicUtil;
import de.linogistix.inventory.gui.component.controls.CustomerOrderComboBoxModel;
import de.linogistix.inventory.gui.component.controls.CustomerOrderPositionComboBoxModel;
import de.linogistix.location.gui.component.controls.LOSStorageLocationComboBoxModel;
import de.linogistix.los.inventory.exception.InventoryException;
import de.linogistix.los.inventory.facade.LOSCompatibilityFacade;
import de.linogistix.los.inventory.query.dto.LOSOrderStockUnitTO;
import de.linogistix.los.model.State;
import de.linogistix.los.query.BODTO;
import de.linogistix.wmsprocesses.processes.treat_order.gui.component.TreatOrderCenterPanel;
import de.linogistix.wmsprocesses.processes.treat_order.gui.model.TreatOrderDialogModel;
import de.linogistix.wmsprocesses.processes.treat_order.gui.model.TreatOrderPickRequestTO;
import de.linogistix.wmsprocesses.processes.treat_order.gui.model.TreatOrderStockSelectionModel;
import de.linogistix.wmsprocesses.res.WMSProcessesBundleResolver;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.delivery.DeliveryOrderLine;
import de.wms2.mywms.product.ItemData;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jordan
 */
public class TreatOrderDialogController {

    private static final Logger log = Logger.getLogger(TreatOrderDialogController.class.getName());
    
    private BOAutoFilteringComboBox<DeliveryOrder> orderCombo;
    private BOAutoFilteringComboBox<DeliveryOrderLine> orderPositionCombo;
    private BOAutoFilteringComboBox<StorageLocation> targetPlaceCombo;
    
    private TreatOrderStockSelectionModel stockSelectionModel;
    private TreatOrderCenterPanel myCenterPanel;
    private TreatOrderPickRequestTO actuPickRequest = null;
    private TreatOrderDialogModel dialogModel;
    
    private ItemMeasure chosenAmount;

    public TreatOrderDialogController(TreatOrderCenterPanel centerPanel,
                                      BOAutoFilteringComboBox<DeliveryOrder> orderCombo,
                                      BOAutoFilteringComboBox<DeliveryOrderLine> orderPositionCombo,
                                      TreatOrderStockSelectionModel stockSelectionModel,
                                      BOAutoFilteringComboBox<StorageLocation> targetPlaceCombo)
            throws Exception 
    {
        myCenterPanel = centerPanel;

        myCenterPanel.getPickRequestListView().setSingleSelection(true);
        myCenterPanel.getPickRequestListView().addSelectionListener(
                new LOSListViewSelectionListener() {

                    @SuppressWarnings("unchecked")
                    public void selectionChanged(List selectedEntities) {
                        if(selectedEntities.size()>0){
                            pickRequestSelected((TreatOrderPickRequestTO)selectedEntities.get(0));
                        }
                    }
                });

        this.orderCombo = orderCombo;
        CustomerOrderComboBoxModel orderModel = new CustomerOrderComboBoxModel();
        orderModel.setOrderState(State.RAW);
        this.orderCombo.setComboBoxModel(orderModel);
        this.orderCombo.addItemChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                orderChanged(evt);
            }
        });

        this.orderPositionCombo = orderPositionCombo;
        this.orderPositionCombo.setComboBoxModel(new CustomerOrderPositionComboBoxModel());

        this.orderPositionCombo.addItemChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {

                orderPositionChanged();     
            }
        });
        
        targetPlaceCombo = myCenterPanel.getTargetPlaceComboBox();
        LOSStorageLocationComboBoxModel tpModel = new LOSStorageLocationComboBoxModel();
        targetPlaceCombo.setComboBoxModel(tpModel);

        myCenterPanel.getStockChooserView().setEnabled(false);

        this.stockSelectionModel = stockSelectionModel;
        this.stockSelectionModel.setDialogController(this);
        
        this.targetPlaceCombo = targetPlaceCombo;

        dialogModel = new TreatOrderDialogModel();
    }

    public void clear() {
        
        actuPickRequest = null;
        
        orderPositionCombo.clear();
        
        orderCombo.clear();
        orderCombo.setEnabled(true);
        ((CustomerOrderComboBoxModel)orderCombo.getComboBoxModel()).setOrderState(State.RAW);
        
        targetPlaceCombo.clear();
        
        stockSelectionModel.clear();
        
        myCenterPanel.getPickRequestListView().clear();
        
        myCenterPanel.getChosenAmountLabel().setText("");
        myCenterPanel.getRequiredAmountLabel().setText("");
        
        myCenterPanel.getClientLabel().setText("");
        myCenterPanel.getItemDataLabel().setText("");
        myCenterPanel.getLotLabel().setText("");
        
        myCenterPanel.getPrefixTextField().setText("");
        
        dialogModel.clear();
        
        myCenterPanel.getStockChooserView().setEnabled(false);
        myCenterPanel.getStockChooserView().reload();
        
        myCenterPanel.getCommentArea().setText("");
        myCenterPanel.warningLabel.setIcon(null);
    }

    private void orderChanged(PropertyChangeEvent evt) {

        orderPositionCombo.clear();

        DeliveryOrder order = orderCombo.getSelectedAsEntity();
        BODTO<DeliveryOrder> orderTO = orderCombo.getSelectedItem();

        if (orderTO != null && order != null) {
            ((CustomerOrderPositionComboBoxModel) orderPositionCombo.getComboBoxModel()).setOrderTO(orderTO);
            orderPositionCombo.setEnabled(true);

            myCenterPanel.clientValueLabel.setText(order.getClient().getName());
            
            if(order.getAdditionalContent() != null && order.getAdditionalContent().length()>0){
                
                myCenterPanel.getCommentArea().setText(order.getAdditionalContent());
                myCenterPanel.getCommentArea().setMargin(new Insets(5, 5, 5, 5));
                
                myCenterPanel.warningLabel.setIcon(GraphicUtil.getInstance().getIcon(IconType.WARNING));
            }
            else{
                myCenterPanel.warningLabel.setIcon(null);
            }
            
            if(order.getDestination() != null){
                
                BODTO<StorageLocation> target;
                target = new BODTO<StorageLocation>(order.getDestination().getId(), 
                                                       order.getDestination().getVersion(), 
                                                       order.getDestination().getName());
                
                targetPlaceCombo.addItem(target);
                targetPlaceCombo.setSelectedItem(target);
            }
            
        } else {
            myCenterPanel.getClientLabel().setText("");
            orderPositionCombo.setEnabled(false);
            myCenterPanel.getCommentArea().setText("");
            myCenterPanel.warningLabel.setIcon(null);
            targetPlaceCombo.clear();
        }

    }

    @SuppressWarnings("unchecked")
    private void orderPositionChanged() {

        DeliveryOrderLine pos = orderPositionCombo.getSelectedAsEntity();
        BODTO<DeliveryOrderLine> posTO = orderPositionCombo.getSelectedItem();

        if (posTO != null && pos != null) {

            stockSelectionModel.setOrderPositionTO(posTO);
            stockSelectionModel.setItemDataTO(new BODTO<ItemData>(pos.getItemData().getId(), 
                                                                  pos.getItemData().getVersion(), 
                                                                  pos.getItemData().getNumber()));

            myCenterPanel.itemDataValueLabel.setText(pos.getItemData().getNumber());
            
            if(!StringUtils.isBlank(pos.getLotNumber())) {
                String presetLot = pos.getLotNumber();
                myCenterPanel.getLotLabel().setText(presetLot);
                ((TreatOrderStockSelectionModel)myCenterPanel.getStockChooserView().getModel()).setLotNumber(presetLot);
                
            }
            else{
                myCenterPanel.getLotLabel().setText("");
                ((TreatOrderStockSelectionModel)myCenterPanel.getStockChooserView().getModel()).setLotNumber(null);
            }

            if (actuPickRequest != null && pos.getAmount().compareTo(pos.getPickedAmount())>0 ) {
                myCenterPanel.getStockChooserView().setEnabled(true);
            }
            else {
                myCenterPanel.getStockChooserView().setEnabled(false);
            }

            dialogModel.setActuOrderPosition(posTO, pos.getAmount().subtract(pos.getPickedAmount()));

            List<BODTO> selPickList = myCenterPanel.getPickRequestListView().getSelectedEntities();

            if (selPickList.size() > 0) {
                List<BODTO<StockUnit>> chosenStockList;
                chosenStockList = dialogModel.getChosenStocks(selPickList.get(0).getName());

                myCenterPanel.getStockChooserView().getModel().clearSelectionList();
                myCenterPanel.getStockChooserView().getModel().setSelectionList(chosenStockList);
            }

            myCenterPanel.getStockChooserView().reload();
            

            BigDecimal amountPos = pos.getAmount();
//            BigDecimal amountPicked = pos.getAmountPicked();
            BigDecimal amountMissing = pos.getAmount().subtract(pos.getPickedAmount());
            BigDecimal amountChoosen = dialogModel.getChosenAmountByOrderId(pos.getId());
//            BigDecimal amountIST = amountPicked.add(amountChoosen);
            chosenAmount = new ItemMeasure(dialogModel.getChosenAmountByOrderId(pos.getId()), pos.getItemData().getItemUnit());

            myCenterPanel.getRequiredAmountLabel().setText( new ItemMeasure(amountMissing, pos.getItemData().getItemUnit()).toString() );
            myCenterPanel.getChosenAmountLabel().setText( new ItemMeasure(amountChoosen, pos.getItemData().getItemUnit()).toString() );
            myCenterPanel.getPosAmountLabel().setText( new ItemMeasure(amountPos, pos.getItemData().getItemUnit()).toString() );
        }

    }

    @SuppressWarnings("unchecked")
    private void pickRequestSelected(TreatOrderPickRequestTO pickRequest) {
        actuPickRequest = pickRequest;

        DeliveryOrderLine pos = orderPositionCombo.getSelectedAsEntity();
        if (pos != null) {
            if (actuPickRequest != null && pos.getAmount().compareTo(pos.getPickedAmount())>0 ) {
                myCenterPanel.getStockChooserView().setEnabled(true);
            }
            else {
                myCenterPanel.getStockChooserView().setEnabled(false);
            }

            List<BODTO<StockUnit>> chosenStockList;
            chosenStockList = dialogModel.getChosenStocks(actuPickRequest.pickRequestNumber);

            myCenterPanel.getStockChooserView().getModel().clearSelectionList();
            myCenterPanel.getStockChooserView().getModel().setSelectionList(chosenStockList);
        }
    }

    public BigDecimal addChosenStock(LOSOrderStockUnitTO selectedStock) {

        BigDecimal chosen = dialogModel.addChosenStock(actuPickRequest, selectedStock);
        
        chosenAmount.setValue(dialogModel.getChosenAmount());
        myCenterPanel.getChosenAmountLabel().setText(chosenAmount.toString());
        
        orderCombo.setEnabled(false);
        
        return chosen;
    }

    @SuppressWarnings("unchecked")
    public void removeChosenStock(LOSOrderStockUnitTO selectedStock) {

       dialogModel.removeChosenStock(selectedStock);
       
        chosenAmount.setValue(dialogModel.getChosenAmount());
        myCenterPanel.getChosenAmountLabel().setText(chosenAmount.toString());
    }

    public void process() {

        try{            
            DeliveryOrder order = orderCombo.getSelectedAsEntity();
            
            if(dialogModel.getHandledPositions().size() != order.getLines().size()){
                
                StringBuffer sb = new StringBuffer("\n");
                
                for(DeliveryOrderLine pos : order.getLines()){
                    
                    boolean handled = false;
                    
                    for(BODTO<DeliveryOrderLine> handledTO : dialogModel.getHandledPositions()){
                        
                        if(pos.getLineNumber().equals(handledTO.getName())){
                            handled = true;
                            continue;
                        }
                    }
                    
                    if(!handled){
                        sb.append(pos.getLineNumber()+"\n");
                    }
                }
                
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(
                                        NbBundle.getMessage(WMSProcessesBundleResolver.class, "ConfirmUnhandled.message", new Object[]{sb.toString()}),
                                        NbBundle.getMessage(WMSProcessesBundleResolver.class,"ConfirmUnhandled.header"),
                                        NotifyDescriptor.YES_NO_OPTION);

                if (DialogDisplayer.getDefault().notify(d) != NotifyDescriptor.YES_OPTION) {
                    return;
                }                
            }          
            
            J2EEServiceLocator loc = Lookup.getDefault().lookup(J2EEServiceLocator.class);        
            LOSCompatibilityFacade pickOrderFacade = loc.getStateless(LOSCompatibilityFacade.class);
            pickOrderFacade.createPickRequests(dialogModel.getChosenStocks());
            
            clear();
        } catch (Throwable t){
            log.log(Level.INFO,t.getMessage(), t);
            ExceptionAnnotator.annotate(t);
        }
    }
    
    public void reset(){
        
        try{
            dialogModel.clearReservation();
        }catch(InventoryException invex){
            ExceptionAnnotator.annotate(invex);
            return;
        }
        
        clear();
    }
}
