/*
 * Copyright (c) 2009-2013 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.businessservice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;
import org.mywms.globals.SerialNoRecordType;
import org.mywms.model.User;

import de.linogistix.los.inventory.customization.ManageOrderService;
import de.linogistix.los.inventory.exception.InventoryException;
import de.linogistix.los.inventory.exception.InventoryExceptionKey;
import de.linogistix.los.model.State;
import de.linogistix.los.util.businessservice.ContextService;
import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.delivery.DeliveryOrderLine;
import de.wms2.mywms.delivery.DeliveryOrderLineStateChangeEvent;
import de.wms2.mywms.delivery.DeliveryOrderStateChangeEvent;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.inventory.InventoryBusiness;
import de.wms2.mywms.inventory.JournalHandler;
import de.wms2.mywms.inventory.StockState;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.location.StorageLocationEntityService;
import de.wms2.mywms.picking.Packet;
import de.wms2.mywms.picking.PacketEntityService;
import de.wms2.mywms.picking.PacketStateChangeEvent;
import de.wms2.mywms.picking.PickingOrder;
import de.wms2.mywms.picking.PickingOrderGenerator;
import de.wms2.mywms.picking.PickingOrderLine;
import de.wms2.mywms.picking.PickingOrderLineEntityService;
import de.wms2.mywms.picking.PickingOrderLineGenerator;
import de.wms2.mywms.picking.PickingOrderLineStateChangeEvent;
import de.wms2.mywms.picking.PickingOrderStateChangeEvent;
import de.wms2.mywms.picking.PickingType;
import de.wms2.mywms.strategy.OrderState;
import de.wms2.mywms.strategy.OrderStateCalculator;
import de.wms2.mywms.user.UserBusiness;

/**
 * 
 * @author krane
 */
@Stateless
public class LOSOrderBusinessBean implements LOSOrderBusiness {
	private Logger log = Logger.getLogger(this.getClass());

	@PersistenceContext(unitName = "myWMS")
	private EntityManager manager;

 	@Inject
	private PickingOrderLineEntityService pickingPositionService;
	@EJB
	private ContextService contextService;
	@EJB
	private ManageOrderService manageOrderService;
	@Inject
	private JournalHandler journalHandler;
	@Inject
	private PickingOrderGenerator pickingOrderGeneratorService;
	@Inject
	private PickingOrderLineGenerator pickingPosGeneratorService;
	@Inject
	private PacketEntityService pickingUnitLoadService;
	@Inject
	private StorageLocationEntityService locationService;
	@Inject
	private InventoryBusiness inventoryBusiness;
	@Inject
	private OrderStateCalculator orderStateCalculator;
	@Inject
	private UserBusiness userBusiness;

	@Inject
	private Event<DeliveryOrderStateChangeEvent> deliveryOrderStateChangeEvent;
	@Inject
	private Event<DeliveryOrderLineStateChangeEvent> deliveryOrderLineStateChangeEvent;
	@Inject
	private Event<PacketStateChangeEvent> packetStateChangeEvent;
	@Inject
	private Event<PickingOrderLineStateChangeEvent> pickingOrderLineStateChangeEvent;
	@Inject
	private Event<PickingOrderStateChangeEvent> pickingOrderStateChangeEvent;

    public DeliveryOrder finishDeliveryOrder(DeliveryOrder deliveryOrder) throws FacadeException {
		String logStr = "finishdeliveryOrder ";
		log.debug(logStr+"orderNumber="+deliveryOrder.getOrderNumber());
		
		if( deliveryOrder.getState()>State.FINISHED ) {
			log.error(logStr+"Finishing of already finished customer order. orderNumber="+deliveryOrder.getOrderNumber()+", state="+deliveryOrder.getState());
			throw new InventoryException(InventoryExceptionKey.ORDER_ALREADY_FINISHED, "");
		}
		if( deliveryOrder.getState()==State.FINISHED ) {
			log.warn(logStr+"Finishing of already finished customer order. Ignore. orderNumber="+deliveryOrder.getOrderNumber()+", state="+deliveryOrder.getState());
			return deliveryOrder;
		}

		int stateOld = deliveryOrder.getState();
		
		boolean hasOnlyCanceledPos = true;
		for( DeliveryOrderLine pos : deliveryOrder.getLines() ) {
			int posStateOld = pos.getState(); 
			if( posStateOld < State.FINISHED ) {
				if( BigDecimal.ZERO.compareTo(pos.getPickedAmount())<0 ) {
					pos.setState(State.FINISHED);
				}
				else {
					pos.setState(State.CANCELED);
				}
				fireDeliveryOrderLineStateChangeEvent(pos, posStateOld);
			}
			if( pos.getState() != State.CANCELED ) {
				hasOnlyCanceledPos = false;
			}
		}
		if( hasOnlyCanceledPos ) {
			deliveryOrder.setState(State.CANCELED);
		}
		else {
			deliveryOrder.setState(State.FINISHED);
		}

		if( deliveryOrder.getState() != stateOld ) {
			fireDeliveryOrderStateChangeEvent(deliveryOrder, stateOld);
		}
		
    	return deliveryOrder;
    }
    
	public DeliveryOrderLine confirmDeliveryOrderLine(DeliveryOrderLine deliveryOrderLine, BigDecimal amount) throws FacadeException {
		String logStr = "confirmDeliveryOrderLine ";
		log.debug(logStr);

		if( amount == null || BigDecimal.ZERO.compareTo(amount)>0 ) {
			log.error(logStr+"No valid amount. Abort. amount="+amount+deliveryOrderLine.getLineNumber()+", state="+deliveryOrderLine.getState());
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_INVALID_AMOUNT, amount==null?"NULL":amount.toString());
		}

		int stateOld = deliveryOrderLine.getState();
		BigDecimal amountRequested = deliveryOrderLine.getAmount();
		BigDecimal amountPicked = deliveryOrderLine.getPickedAmount();
		amountPicked = amountPicked.add(amount);
		
		deliveryOrderLine.setPickedAmount(amountPicked);

		if( deliveryOrderLine.getState()>=State.PICKED ) {
			log.warn(logStr+"Added picked amount to already finished delivery order line. line="+deliveryOrderLine.getLineNumber()+", state="+deliveryOrderLine.getState()+", added amount="+amount+", new picked amount="+amountPicked);
		}

		if( amountRequested.compareTo(amountPicked) <= 0 ) {
			if( deliveryOrderLine.getState()<State.PICKED ) {
				deliveryOrderLine.setState(State.PICKED);
			}
		}
		else {
			// Find open picks. If no more picks are available, the state is set to PENDING
			boolean hasOpenPicks = false;
			List<PickingOrderLine> pickList = pickingPositionService.readByDeliveryOrderLine(deliveryOrderLine);
			for( PickingOrderLine pick : pickList ) {
				if( pick.getState() < State.PICKED ) {
					hasOpenPicks = true;
					break;
				}
			}
			if( deliveryOrderLine.getState()<State.PICKED ) {
				if( hasOpenPicks ) {
					deliveryOrderLine.setState(State.STARTED);
				}
				else {
					deliveryOrderLine.setState(State.PENDING);
				}
			}
		}
		
		DeliveryOrder deliveryOrder = deliveryOrderLine.getDeliveryOrder();
		int orderStateOld = deliveryOrder.getState();
		if (deliveryOrder.getState() < State.STARTED && amountPicked.compareTo(BigDecimal.ZERO) != 0) {
			deliveryOrder.setState(State.STARTED);

		}
		
		if( deliveryOrderLine.getState() != stateOld )  {
			fireDeliveryOrderLineStateChangeEvent(deliveryOrderLine, stateOld);
		}

		if( deliveryOrderLine.getState()>=State.PENDING && deliveryOrder.getState()<State.PICKED ) {
			// Check state of order
			boolean hasAllPicked = true;
			boolean hasPendingPicks = false;
			for( DeliveryOrderLine cop : deliveryOrder.getLines() ) {
				if( cop.getState() == State.PENDING ) {
					hasPendingPicks = true;
				}
				if( cop.getState()<State.PENDING ) {
					hasAllPicked = false;
					break;
				}
			}

			if( hasAllPicked ) {
				if( hasPendingPicks ) {
					log.info(logStr+"Found pending picks for order. number="+deliveryOrder.getOrderNumber());
					
					deliveryOrder.setState(State.PENDING);
					
				}
				else {
					log.info(logStr+"Everything picked for order. Confirm order. number="+deliveryOrder.getOrderNumber());
					deliveryOrder.setState(State.PICKED);

				}
			}
		}
		
		if( deliveryOrder.getState() != orderStateOld )  {
			fireDeliveryOrderStateChangeEvent(deliveryOrder, stateOld);
		}

		return deliveryOrderLine;
	}
	
	public PickingOrder releasePickingOrder(PickingOrder pickingOrder) throws FacadeException {
		String logStr = "releasePickingOrder ";
		log.debug(logStr+"order="+pickingOrder);
		
		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		
		int stateOld = pickingOrder.getState();
		
		if( stateOld>=State.PICKED ) {
			log.error(logStr+"Order is already picked. => Cannot release.");
			throw new InventoryException(InventoryExceptionKey.PICK_ALREADY_FINISHED, "");
		}
		
		if( stateOld>=State.PROCESSABLE ) {
			log.warn(logStr+"Order is already released. => Do nothing.");
			return pickingOrder;
		}

		if( !manageOrderService.isPickingOrderReleasable(pickingOrder) ) {
			log.info(logStr+"Order must not be released. => Do nothing.");
			return pickingOrder;
		}
		
		pickingOrder.setState(State.PROCESSABLE);
		
		firePickingOrderStateChangeEvent(pickingOrder, stateOld);

		return pickingOrder;
	}
	
	public PickingOrder haltPickingOrder(PickingOrder pickingOrder) throws FacadeException {
		String logStr = "haltPickingOrder ";
		log.debug(logStr+"order="+pickingOrder);
		
		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		
		int stateOld = pickingOrder.getState();

		if( pickingOrder.getState()>State.RESERVED ) {
			log.error(logStr+"Order is already in progress. Cannot halt");
			throw new InventoryException(InventoryExceptionKey.PICK_ALREADY_STARTED, "");
		}
		
		pickingOrder.setState(State.RAW);
		pickingOrder.setOperator(null);
		
		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}

		return pickingOrder;
	}
	
	/**
	 * Reserves the picking order for the given user.<br>
	 * If reservation is not possible, an exception is thrown.
	 * 
	 * @param pickingOrder
	 * @param user
	 * @param force If TRUE, a reservation for a different user will be ignored
	 * @return
	 * @throws FacadeException
	 */
	public PickingOrder reservePickingOrder(PickingOrder pickingOrder, User user, boolean ignoreReservationGap) throws FacadeException {
		String logStr = "reservePickingOrder ";
		log.debug(logStr+"order="+pickingOrder+", ignoreReservationGap="+ignoreReservationGap);
		
		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		
		int stateOld = pickingOrder.getState();

		if( pickingOrder.getState()<State.PROCESSABLE) {
			log.error(logStr+"Order not yet processable. => Cannot start.");
			throw new InventoryException(InventoryExceptionKey.ORDER_RESERVED, "");
		}
		if( stateOld>=State.PICKED ) {
			log.error(logStr+"Order is already picked. => Cannot reserve.");
			throw new InventoryException(InventoryExceptionKey.PICK_ALREADY_STARTED, pickingOrder.getOrderNumber());
		}
		if( user == null ) {
			user = contextService.getCallersUser();
		}
		if( !ignoreReservationGap && (stateOld>=State.RESERVED) && (pickingOrder.getOperator() != null) && (!pickingOrder.getOperator().equals(user)) ) {
			log.error(logStr+"Order is already assigned to a different user. => Cannot reserve.");
			throw new InventoryException(InventoryExceptionKey.ORDER_RESERVED, "");
		}

		pickingOrder.setOperator(user);
		if( stateOld<State.PROCESSABLE ) {
			if( manageOrderService.isPickingOrderReleasable(pickingOrder) ) {
				pickingOrder.setState(State.RESERVED);
			}
			else {
				log.info(logStr+"Order must not be released.");
			}
		}
		else if( stateOld<State.RESERVED) {
			pickingOrder.setState(State.RESERVED);
		}

		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}

		return pickingOrder;
	}

	public PickingOrder startPickingOrder(PickingOrder pickingOrder, boolean ignoreReservationGap) throws FacadeException {
		String logStr = "startPickingOrder ";
		log.debug(logStr+"order="+pickingOrder);

		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		
		int stateOld = pickingOrder.getState();

		if( pickingOrder.getState()<State.PROCESSABLE) {
			log.error(logStr+"Order not yet processable. => Cannot start.");
			throw new InventoryException(InventoryExceptionKey.ORDER_RESERVED, "");
		}
		if( pickingOrder.getState()>=State.PICKED) {
			log.error(logStr+"Order is already picked. => Cannot start.");
			throw new InventoryException(InventoryExceptionKey.ORDER_ALREADY_FINISHED, "");
		}
		User user = contextService.getCallersUser();
		if( !ignoreReservationGap && (pickingOrder.getState()>=State.RESERVED) && (pickingOrder.getOperator() != null) && (!pickingOrder.getOperator().equals(user)) ) {
			log.error(logStr+"Order is already assigned to a different user. => Cannot reserve.");
			throw new InventoryException(InventoryExceptionKey.ORDER_RESERVED, "");
		}
		pickingOrder.setOperator(user);
		if( pickingOrder.getState()<State.STARTED) {
			pickingOrder.setState(State.STARTED);
		}
		
		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}

		return pickingOrder;
	}
	
	public PickingOrder resetPickingOrder(PickingOrder pickingOrder) throws FacadeException {
		String logStr = "resetPickingOrder ";
		log.debug(logStr+"order="+pickingOrder);
		
		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		int stateOld = pickingOrder.getState();

		if( pickingOrder.getState()>State.PICKED ) {
			log.error(logStr+"Order is already picked. => Cannot reset.");
			throw new InventoryException(InventoryExceptionKey.ORDER_ALREADY_FINISHED, "");
		}
		
		boolean hasFinishedPicks = false;
		boolean hasOpenPicks = false;
		for( PickingOrderLine pick : pickingOrder.getLines() ) {
			int pickStateOld = pick.getState(); 
			if( pickStateOld >= State.PICKED ) {
				hasFinishedPicks = true;
				// do not change already picked positions
				continue;
			}
			hasOpenPicks = true;
			if( pickStateOld <= State.PROCESSABLE) {
				// do not change already reseted positions
				continue;
			}
			
			pick.setState(State.PROCESSABLE);
			firePickingOrderLineStateChangeEvent(pick, pickStateOld);
		}
		List<PickingOrderLine> openPickList = new ArrayList<PickingOrderLine>();
		
		if( hasFinishedPicks ) {
			if( hasOpenPicks ) {
				// Remove open picks from the order
				// Set them back to the pool to generate a new picking order
				for( PickingOrderLine pick : pickingOrder.getLines() ) {
					int pickStateOld = pick.getState(); 
					if( pickStateOld >= State.PICKED ) {
						continue;
					}
					pick.setPickingOrder(null);
					if( pickStateOld >= State.PROCESSABLE) {
						pick.setState(State.ASSIGNED);
						firePickingOrderLineStateChangeEvent(pick, pickStateOld);
						openPickList.add(pick);
					}
				}
			}
			if( pickingOrder.getState()<State.FINISHED ) {
				pickingOrder.setState(State.FINISHED);
			}
		}
		else if( hasOpenPicks ) {
			if( pickingOrder.getState()>State.PROCESSABLE ) {
				pickingOrder.setState(State.PROCESSABLE);
				pickingOrder.setOperator(null);
			}
		}
		else {
			if( pickingOrder.getState()<State.FINISHED ) {
				pickingOrder.setState(State.CANCELED);
			}
		}
		
		if( openPickList.size()>0 ) {
			log.debug(logStr+"Create new picking order");
			
			// Take prefix of number to the new generated picking order.
			// This only works for basic, not configurable picking order numbers
			String numberOld = pickingOrder.getOrderNumber();
			String prefix = "";
			int idx = numberOld.lastIndexOf("_PICK ");
			if( idx>0 && idx<numberOld.length() ) {
				prefix=numberOld.substring(0, idx)+"_";
			}
			
			Collection<PickingOrder> newOrderList;
			newOrderList = pickingOrderGeneratorService.generatePickingOrders(openPickList);
			for( PickingOrder newOrder : newOrderList ) {
				newOrder.setOrderNumber(prefix+newOrder.getOrderNumber());
				releasePickingOrder(newOrder);
			}
		}
		
		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}

		return pickingOrder;
	}

	/**
	 * Finishes a picking order in the current state.<br>
	 * Not finished unit loads of the picking order are moved to the clearing location.<br> 
	 * All not picked positions are moved back to the pool. 
	 * 
	 * @param pickingOrder
	 * @throws FacadeException
	 */
	public PickingOrder finishPickingOrder(PickingOrder pickingOrder) throws FacadeException {
		String logStr = "finishPickingOrder ";
		log.debug(logStr+"order="+pickingOrder);

		if( pickingOrder == null ) {
			log.error(logStr+"missing parameter order");
			return null;
		}
		int stateOld = pickingOrder.getState();

		if( pickingOrder.getState() >= State.FINISHED ) {
			log.error(logStr+"Order is already finished. => Cannot finish.");
			throw new InventoryException(InventoryExceptionKey.ORDER_ALREADY_FINISHED, "");
		}
		
		int orderState = State.CANCELED;
		for( PickingOrderLine pick : pickingOrder.getLines() ) {
			if( pick.getState()<State.PICKED ) {
				cancelPick(pick);
			}
			if( pick.getState() >= State.PICKED && pick.getState() != State.CANCELED ) {
				orderState = State.FINISHED;
			}
		}
		
		pickingOrder.setState(orderState);

		// cleanup unit loads
		// Not finished unit loads on the users location are moved to CLEARING
		User user = pickingOrder.getOperator();
		if( user != null ) {
			List<Packet> ulList = pickingUnitLoadService.readByPickingOrder(pickingOrder);
			if( ulList != null && ulList.size()>0 ) {
				log.debug(logStr+"Cleanup unit loads on users location. userName="+user.getName());
				StorageLocation usersLocation = locationService.getCurrentUsersLocation();
				for( Packet unitLoad : ulList ) {
					if( unitLoad.getUnitLoad().getStorageLocation().equals(usersLocation) ) {
						inventoryBusiness.transferToClearing(unitLoad.getUnitLoad(), pickingOrder.getOrderNumber(),
								pickingOrder.getOperator(), null);
					}
				}
			}
		}		

		orderStateCalculator.calculateDeliveryOrderState(pickingOrder);

		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}
		
		return pickingOrder;
	}

	public void confirmPick(PickingOrderLine pick, Packet pickToUnitLoad, BigDecimal amountPicked, BigDecimal amountRemain, List<String> serialNoList) throws FacadeException {
		confirmPick(pick, pickToUnitLoad, amountPicked, amountRemain, serialNoList, false);
	}
	
	public void confirmPick(PickingOrderLine pick, Packet pickToUnitLoad, BigDecimal amountPicked, BigDecimal amountRemain, List<String> serialNoList, boolean counted) throws FacadeException {
		String logStr = "confirmPick ";
		if( pick == null ) {
			log.error(logStr+"missing parameter pick");
			return;
		}
		log.debug(logStr+"pick.id="+pick.getId()+", item="+pick.getItemData().getNumber()+", amount="+amountPicked+", remain="+amountRemain+", unitLoad="+pickToUnitLoad);
		int stateOld = pick.getState();

		if( pick.getState()>=State.PICKED ) {
			log.error(logStr+"Pick already done. => Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_ALREADY_FINISHED, "");
		}
		if( pick.getPickFromStockUnit() == null ) {
			log.error(logStr+"Pick has no assigned stock. => Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_NO_STOCK, "");
		}
		if( amountPicked == null ) {
			amountPicked = pick.getAmount();
		}
		if( BigDecimal.ZERO.compareTo(amountPicked) > 0 ) {
			log.error(logStr+"Impossible to pick negative amount. Abort");
			throw new InventoryException(InventoryExceptionKey.AMOUNT_MUST_BE_GREATER_THAN_ZERO, "");
		}
		if( BigDecimal.ZERO.compareTo(amountPicked) >= 0 && amountRemain == null ) {
			log.error(logStr+"Nothing picked and nothing counted. Impossible. Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_NOT_PICKED_COUNTED, "");
		}

		PickingOrder pickingOrder = pick.getPickingOrder();
		if( pickingOrder == null ) {
			log.error(logStr+"Cannot confirm without order. Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_MISSING_ORDER, "");
		}
		
		if( pickToUnitLoad == null && BigDecimal.ZERO.compareTo(amountPicked)<0 ) {
			log.error(logStr+"Cannot confirm without unit load. Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_MISSING_UNITLOALD, "");
		}

		if( pickToUnitLoad != null ) {
			if( pickToUnitLoad.getPickingOrder()==null ) {
				log.error(logStr+"Correction of pickingOrder in PickingUnitLoad. Was empty. pickToUnitLoad="+pickToUnitLoad+", pickingOrder="+pickingOrder); 
				pickToUnitLoad.setPickingOrder(pickingOrder);
			}
			if (pickToUnitLoad.getAddress() == null) {
				pickToUnitLoad.setAddress(pickingOrder.getAddress());
			}
			if (StringUtils.isBlank(pickToUnitLoad.getCarrierName())) {
				pickToUnitLoad.setCarrierName(pickingOrder.getCarrierName());
				pickToUnitLoad.setCarrierService(pickingOrder.getCarrierService());
			}
			if( pickToUnitLoad.getPickingOrder()==null || !pickToUnitLoad.getPickingOrder().equals(pickingOrder) ) {
				log.error(logStr+"Wrong unit load picking order number. Abort. Actual="+pickToUnitLoad.getPickingOrder().getOrderNumber()+", Requested="+pickingOrder.getOrderNumber());
				throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_WRONG_UNITLOAD, new Object[]{pickToUnitLoad.getPickingOrder().getOrderNumber(), pickingOrder.getOrderNumber()});
			}
			if (pick.getDeliveryOrderLine() != null && pickToUnitLoad.getDeliveryOrder() == null) {
				DeliveryOrder deliveryOrder = findUniqueDeliveryOrder(pickingOrder);
				if (deliveryOrder != null) {
					pickToUnitLoad.setDeliveryOrder(deliveryOrder);
					if (pickToUnitLoad.getAddress() == null) {
						pickToUnitLoad.setAddress(deliveryOrder.getAddress());
					}
					if (StringUtils.isBlank(pickToUnitLoad.getCarrierName())) {
						pickToUnitLoad.setCarrierName(deliveryOrder.getCarrierName());
						pickToUnitLoad.setCarrierService(deliveryOrder.getCarrierService());
					}
				}
			}
		}

		String activityCode = pickingOrder.getOrderNumber();
		StockUnit pickFromStock = pick.getPickFromStockUnit();
		BigDecimal amountPickFrom = pickFromStock.getAmount();
		String lotPicked = pickFromStock.getLotNumber();
		
		// Release the reservation on pick from stock unit. 
		// Maybe that some services would fail on the original reservations
		pickFromStock.releaseReservedAmount(pick.getAmount());

		// Check amount differences
		if( amountRemain == null ) {
			if( amountPicked.compareTo(amountPickFrom) > 0 ) {
				log.info(logStr+"More stock picked as available. Adjust amount. Picked="+amountPicked+", Available="+amountPickFrom);
				changeAmount(pickFromStock, amountPicked, activityCode);
			}
		}
		else {
			
			UnitLoad pickFromUnitLoad = pickFromStock.getUnitLoad();
			StorageLocation pickFromLocation = pickFromUnitLoad.getStorageLocation();
			
			if( BigDecimal.ZERO.compareTo(amountRemain) > 0 ) {
				amountRemain = BigDecimal.ZERO;
			}
			BigDecimal amountStart = amountRemain.add(amountPicked);
			log.info(logStr+"Stock has been counted. Remaining="+amountRemain+", Start="+amountStart);
			if( amountStart.compareTo(amountPickFrom) > 0 ) {
				// There is more stock than expected on the pick from stock unit
				// Adjust it to the correct value
				log.info(logStr+"Adjust amount before picking. Desired="+amountPickFrom+", Counted="+amountStart);
				changeAmount(pickFromStock, amountStart, activityCode);
				amountPickFrom = amountStart;
			}
			else if( amountStart.compareTo(amountPickFrom) < 0 ) {
				// Some amount on pick from stock unit is missing
				// Adjust it to the correct value
				log.info(logStr+"Missing amout. Adjust before picking. Desired="+amountPickFrom+", Counted="+amountStart);
				
				// Remove temporal from follow up pick generation
				pickFromStock.setReservedAmount(pickFromStock.getAmount());
				
				// Maybe other picking positions are affected
				// If the pick from stock is going to zero, other affected picks are reseted
				if( BigDecimal.ZERO.compareTo(amountRemain) == 0 ) {
					List<PickingOrderLine> affectedList = pickingPositionService.readBySourceStockUnit(pickFromStock);
					for( PickingOrderLine affected : affectedList ) {
						if( affected.equals(pick) ) {
							continue;
						}
						cancelPick(affected);

						// Remove temporal from follow up pick generation
						pickFromStock.setReservedAmount(pickFromStock.getAmount());
						
						PickingOrder affectedOrder = affected.getPickingOrder();
						if (affectedOrder != null && affectedOrder.isCreateFollowUpPicks()) {
							DeliveryOrderLine orderPos = affected.getDeliveryOrderLine();
							if( orderPos != null ) {
								List<PickingOrderLine> pickListNew = pickingPosGeneratorService.generatePicks(orderPos,
										pickingOrder.getOrderStrategy(), affected.getAmount());
								pickingOrderGeneratorService.addPicksToOrder(affectedOrder, pickListNew);
							}
						}
					}
				}

				pickFromStock.setReservedAmount(BigDecimal.ZERO);
				changeAmount(pickFromStock, amountStart, activityCode);
				amountPickFrom = amountStart;
			}
			
			if( counted ) {
				if( pickFromLocation.getUnitLoads().size()<2 ) {
					pickFromLocation.setStockTakingDate(new Date());
				}
				User operator = contextService.getCallersUser();
				journalHandler.recordCounting(pickFromStock, pickFromUnitLoad, pickFromLocation, activityCode, operator, null);
			}
		}

		BigDecimal amountPosted = BigDecimal.ZERO;

		if( BigDecimal.ZERO.compareTo(amountPicked)<0 ) {
			// move stock to pick-to unit load
			
			// separate serial numbers 
			if( pick.getItemData().getSerialNoRecordType() == SerialNoRecordType.GOODS_OUT_RECORD ) {
				int numSerialRequired = amountPicked.intValue();
				if( serialNoList == null || serialNoList.size()!=numSerialRequired ) {
					log.warn(logStr+"There are not enough serialnumbers");
					throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_MISSING_SERIAL, "");
				}
				
				for( String serial : serialNoList ) {
					StockUnit stock = moveStock(pickFromStock, BigDecimal.ONE, pickToUnitLoad.getUnitLoad(), activityCode);
					stock.setSerialNumber(serial);
					amountPosted = amountPosted.add(BigDecimal.ONE);
				}
				
			}
			else {
				moveStock(pickFromStock, amountPicked, pickToUnitLoad.getUnitLoad(), activityCode);
				amountPosted = amountPosted.add(amountPicked);
			}
			pick.setState(State.PICKED);
			pick.setPacket(pickToUnitLoad);
		}
		else {
			pick.setState(State.CANCELED);
			pick.setPacket(null);
		}

		pick.setPickedAmount(amountPosted);
		pick.setPickedLotNumber(lotPicked);
		pick.setPickFromStockUnit(null);
		pick.setPickingType(PickingType.PICK);

		DeliveryOrderLine deliveryOrderLine = pick.getDeliveryOrderLine();
		if( deliveryOrderLine != null ) {
			if( pickingOrder.isCreateFollowUpPicks()) {
				BigDecimal amountMissing = pick.getAmount().subtract(amountPicked);
				if( BigDecimal.ZERO.compareTo(amountMissing)<0 ) {
					List<PickingOrderLine> pickListNew;
					pickListNew = pickingPosGeneratorService.generatePicks(deliveryOrderLine,
							pickingOrder.getOrderStrategy(), amountMissing);

					if (pickListNew != null && pickListNew.size() > 0) {
						pickingOrderGeneratorService.addPicksToOrder(pickingOrder, pickListNew);
					}
				}
			}		
			confirmDeliveryOrderLine(deliveryOrderLine, amountPicked);
		}

		if( pickingOrder.getState()<State.STARTED ) {
			startPickingOrder(pickingOrder, true);
		}
		
		if( pick.getState() != stateOld )  {
			firePickingOrderLineStateChangeEvent(pick, stateOld);
		}

		
		stateOld = pickToUnitLoad.getState();
		if( stateOld < State.STARTED ) {
			pickToUnitLoad.setState(State.STARTED);
			firePacketStateChangeEvent(pickToUnitLoad, stateOld);
		}
		
		// Check picking order state
		boolean allPicksDone = true;
		for( PickingOrderLine p : pickingOrder.getLines() ) {
			if( p.getState() < State.PICKED ) {
				allPicksDone = false;
				break;
			}
		}
		stateOld = pickingOrder.getState();
		if( allPicksDone && stateOld < State.PICKED ) {
			pickingOrder.setState(State.PICKED);
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}
		

	}

	public void confirmCompletePick(PickingOrderLine pick, StorageLocation destination) throws FacadeException {
		String logStr = "confirmPick ";
		if (pick == null) {
			log.error(logStr + "missing parameter pick");
			return;
		}
		log.debug(logStr + "pick.id=" + pick.getId() + ", item=" + pick.getItemData().getNumber());
		int stateOld = pick.getState();

		if (pick.getState() >= State.PICKED) {
			log.error(logStr + "Pick already done. => Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_ALREADY_FINISHED, "");
		}
		if (pick.getPickFromStockUnit() == null) {
			log.error(logStr + "Pick has no assigned stock. => Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_NO_STOCK, "");
		}

		PickingOrder pickingOrder = pick.getPickingOrder();
		if (pickingOrder == null) {
			log.error(logStr + "Cannot confirm without order. Abort");
			throw new InventoryException(InventoryExceptionKey.PICK_CONFIRM_MISSING_ORDER, "");
		}

		StockUnit pickFromStock = pick.getPickFromStockUnit();
		String lotPicked = pickFromStock.getLotNumber();

		// Release the reservation on pick from stock unit.
		// Maybe that some services would fail on the original reservations
		pickFromStock.releaseReservedAmount(pick.getAmount());

		UnitLoad pickFromUnitLoad = pickFromStock.getUnitLoad();

		Packet packet = pickingUnitLoadService.create(pickFromUnitLoad);
		packet.setState(OrderState.PICKED);
		packet.setPickingOrder(pickingOrder);
		if (packet.getAddress() == null) {
			packet.setAddress(pickingOrder.getAddress());
		}
		if (StringUtils.isBlank(packet.getCarrierName())) {
			packet.setCarrierName(pickingOrder.getCarrierName());
			packet.setCarrierService(pickingOrder.getCarrierService());
		}

		DeliveryOrderLine deliveryOrderLine = pick.getDeliveryOrderLine();
		if (deliveryOrderLine != null) {
			DeliveryOrder deliveryOrder = deliveryOrderLine.getDeliveryOrder();
			if (deliveryOrder != null) {
				packet.setDeliveryOrder(deliveryOrder);
				if (packet.getAddress() == null) {
					packet.setAddress(deliveryOrder.getAddress());
				}
				if (StringUtils.isBlank(packet.getCarrierName())) {
					packet.setCarrierName(deliveryOrder.getCarrierName());
					packet.setCarrierService(deliveryOrder.getCarrierService());
				}
			}
		}

		pick.setState(State.PICKED);
		pick.setPacket(packet);
		pick.setPickedAmount(pickFromStock.getAmount());
		pick.setPickedLotNumber(lotPicked);
		pick.setPickFromStockUnit(null);
		pick.setPickingType(PickingType.COMPLETE);

		if (deliveryOrderLine != null) {
			confirmDeliveryOrderLine(deliveryOrderLine, pickFromStock.getAmount());
		}

		if (pickingOrder.getState() < State.STARTED) {
			startPickingOrder(pickingOrder, true);
		}

		if (pickFromUnitLoad.getState() <= OrderState.PICKED) {
			inventoryBusiness.changeState(pickFromUnitLoad, OrderState.PICKED);
		}

		String activityCode = pickingOrder.getOrderNumber();
		User operator = userBusiness.getCurrentUser();

		// change client
		if (!pickFromUnitLoad.getClient().equals(pickingOrder.getClient())) {
			log.info(logStr + "switch client. unitLoad=" + pickFromUnitLoad + ", from client="
					+ pickFromUnitLoad.getClient() + ", to client=" + pickingOrder.getClient());
			inventoryBusiness.changeClient(pickFromUnitLoad, pickingOrder.getClient(), activityCode, operator, null);
		}

		firePacketStateChangeEvent(packet, -1);
		if (pick.getState() != stateOld) {
			firePickingOrderLineStateChangeEvent(pick, stateOld);
		}

		// Check picking order state
		boolean allPicksDone = true;
		for (PickingOrderLine p : pickingOrder.getLines()) {
			if (p.getState() < State.PICKED) {
				allPicksDone = false;
				break;
			}
		}
		stateOld = pickingOrder.getState();
		if (allPicksDone && stateOld < State.PICKED) {
			pickingOrder.setState(State.PICKED);
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}

		if (!pickFromUnitLoad.getStorageLocation().equals(destination)) {
			inventoryBusiness.transferUnitLoad(pickFromUnitLoad, destination, activityCode, operator, null);
		}
	}

	@SuppressWarnings("unchecked")
	private DeliveryOrder findUniqueDeliveryOrder(PickingOrder pickingOrder) {
		String jpql = "select distinct pick.deliveryOrderLine.deliveryOrder from ";
		jpql += PickingOrderLine.class.getSimpleName() + " pick";
		jpql += " where pick.pickingOrder=:pickingOrder";
		Query query = manager.createQuery(jpql);
		query.setParameter("pickingOrder", pickingOrder);
		List<DeliveryOrder> deliveryOrders = query.getResultList();
		if (deliveryOrders.size() == 1) {
			return deliveryOrders.get(0);
		}
		return null;
	}

	/**
	 * Confirm one picking position.
	 * 
	 * @param pick
	 * @param amountPicked
	 * @param amountRemain
	 * @throws FacadeException
	 */
	public void haltPickingPosition(PickingOrderLine pick) throws FacadeException {
		String logStr = "haltPickingPosition ";
		if( pick == null ) {
			log.error(logStr+"missing parameter pick");
			return;
		}
		log.debug(logStr+"pick.id="+pick.getId()+", item="+pick.getItemData().getNumber());
		int stateOld = pick.getState();

		if( pick.getState()>=State.PICKED ) {
			log.warn(logStr+"Pick already done. => Ignore");
			return;
		}
		if( pick.getState()<=State.ASSIGNED ) {
			log.warn(logStr+"Pick already postponed. => Ignore");
			return;
		}
		
		pick.setState(State.ASSIGNED);
		
		firePickingOrderLineStateChangeEvent(pick, stateOld);
	}

	/**
	 * Cancellation of a single picking position.<br>
	 * The picking order is not affected or recalculated!
	 * 
	 * @param pick
	 * @throws FacadeException
	 */
	public PickingOrderLine cancelPick(PickingOrderLine pick) throws FacadeException {
		String logStr = "cancelPick ";
		if( pick == null ) {
			log.error(logStr+"missing parameter order");
			return pick;
		}
		if( pick.getState()>=State.PICKED ) {
			log.info(logStr+"Cannot cancel already picked pick. id="+pick.getId());
			return pick;
		}
		int stateOld = pick.getState();

		StockUnit pickFromStock = pick.getPickFromStockUnit();
		if( pickFromStock != null ) {
			releaseReservation( pickFromStock, pick.getAmount());
		}
		pick.setState(State.CANCELED);
		pick.setPickFromStockUnit(null);
		
		if( pick.getState() != stateOld )  {
			firePickingOrderLineStateChangeEvent(pick, stateOld);
		}
		
		if( pick.getDeliveryOrderLine() != null ) {
			confirmDeliveryOrderLine(pick.getDeliveryOrderLine(), BigDecimal.ZERO);
		}

		return pick;
	}
	/**
	 * Changes the pick-from stock unit.
	 * 
	 * @param pick
	 * @param pickFromStockUnit
	 * @throws FacadeException
	 */
	public PickingOrderLine changePickFromStockUnit(PickingOrderLine pick, StockUnit pickFromStockNew) throws FacadeException {
		String logStr = "changePickFromStockUnit ";
		
		if( pick == null ) {
			log.error(logStr+"missing parameter pick. => Abort");
			throw new InventoryException(InventoryExceptionKey.MISSING_PARAMETER, "pick");
		}
		if( pickFromStockNew == null ) {
			log.error(logStr+"missing parameter pickFromStockUnit. => Abort");
			throw new InventoryException(InventoryExceptionKey.MISSING_PARAMETER, "pickFromStockNew");
		}

		StockUnit pickFromStockOld = pick.getPickFromStockUnit();
		if( pickFromStockOld == null ) {
			log.error(logStr+"pick has no pickFromStockUnit. => Abort");
			throw new InventoryException(InventoryExceptionKey.MISSING_PARAMETER, "pickFromStockOld");
		}

		UnitLoad pickFromUnitLoadOld = pick.getPickFromStockUnit().getUnitLoad();
		UnitLoad pickFromUnitLoadNew = pickFromStockNew.getUnitLoad();
		
		BigDecimal amountPick = pick.getAmount();
		BigDecimal amountStockNew = pickFromStockNew.getAmount();
		BigDecimal amountReservedNew = pickFromStockNew.getReservedAmount();
		if( amountReservedNew==null )
			amountReservedNew = BigDecimal.ZERO;
		BigDecimal amountStockOld = pickFromStockOld.getAmount();
		BigDecimal amountReservedOld = pickFromStockOld.getReservedAmount();
		if( amountReservedOld==null )
			amountReservedOld = BigDecimal.ZERO;
		amountReservedOld = amountReservedOld.subtract(amountPick);
		BigDecimal amountAvailableOld = amountStockOld.subtract(amountReservedOld);
		
		
		// calculate the maximum of allowed reserved amount on the new stock
		BigDecimal amountReservedMax = BigDecimal.ZERO;
		if( pick.getPickingType() == PickingType.COMPLETE ) {
			amountReservedMax = null;
		}
		else {
			amountReservedMax = amountStockNew.subtract(amountPick);
		}
		if( amountReservedMax!=null && BigDecimal.ZERO.compareTo(amountReservedMax) > 0 ) {
			log.error(logStr+"Not enough material on new stock");
			throw new InventoryException(InventoryExceptionKey.UNSUFFICIENT_AMOUNT, new Object[]{amountStockNew, pick.getItemData().getNumber()});
		}
		
		if( amountReservedMax==null || amountReservedNew.compareTo(amountReservedMax) > 0 ) {
			// Try to change reservations

			List<PickingOrderLine> reserverList = pickingPositionService.readBySourceStockUnit(pickFromStockNew);
			if( reserverList == null || reserverList.size() == 0 ) {
				log.warn(logStr+"seems to be a phantom reservation. kill it. stock="+pickFromStockNew.toDescriptiveString());
				// seems to be a phantom reservation. kill it.
				pickFromStockNew.setReservedAmount(BigDecimal.ZERO);
				amountReservedNew = BigDecimal.ZERO;
			}
			else {
				log.debug(logStr+"check reservers. size="+reserverList.size());
				for( PickingOrderLine reserver : reserverList ) {
					BigDecimal amountReserver = reserver.getAmount();
					if( amountReserver.compareTo(amountAvailableOld)<= 0 ) {
						// Pick can be switched
						reserver.setPickFromStockUnit(pickFromStockOld);
						reserver.setPickFromLocationName(pickFromUnitLoadOld.getStorageLocation().getName());
						reserver.setPickFromUnitLoadLabel(pickFromUnitLoadOld.getLabelId());
						
						amountReservedOld = amountReservedOld.add(amountReserver);
						amountAvailableOld = amountAvailableOld.subtract(amountReserver);
						amountReservedNew = amountReservedNew.subtract(amountReserver);
					}
					if( amountReservedMax!=null && amountReservedNew.compareTo(amountReservedMax) <= 0 ) {
						// It has been remove enough reserved amount
						break;
					}
				}
			}
		}
		
		if( amountReservedMax!=null && amountReservedNew.compareTo(amountReservedMax) > 0 ) {
			// It was not possible to remove enough reserved amount
			log.error(logStr+"Cannot remove enough reserved amount. => Abort. amount-stock="+amountStockNew+", amount-pick="+amountPick+", amount-reserved-max="+amountReservedMax+", amount-reserved-new="+amountReservedNew);
			throw new InventoryException(InventoryExceptionKey.UNSUFFICIENT_AMOUNT, new Object[]{amountStockNew, pick.getItemData().getNumber()});
		}

		amountReservedNew = amountReservedNew.add(amountPick);
		pickFromStockNew.setReservedAmount(amountReservedNew);
		pickFromStockOld.setReservedAmount(amountReservedOld);
		
		pick.setPickFromStockUnit(pickFromStockNew);
		pick.setPickFromLocationName(pickFromUnitLoadNew.getStorageLocation().getName());
		pick.setPickFromUnitLoadLabel(pickFromUnitLoadNew.getLabelId());
		
		return pick;
	}

	
	public Packet confirmPickingUnitLoad( Packet pickingUnitLoad, StorageLocation destination, int state ) throws FacadeException {
		
		UnitLoad unitLoad = pickingUnitLoad.getUnitLoad();
		String activityCode = null;
		PickingOrder pickingOrder = pickingUnitLoad.getPickingOrder();
		if (pickingOrder != null) {
			activityCode = pickingOrder.getOrderNumber();
		}

		if( ! unitLoad.getStorageLocation().equals(destination) ) {
			// Transfer posting only necessary if location is changed
			inventoryBusiness.transferUnitLoad(pickingUnitLoad.getUnitLoad(), destination, activityCode, null, null);
		}

		pickingUnitLoad.getUnitLoad().setState(StockState.PICKED);
		int stateOld = pickingUnitLoad.getState();
		if( state < 0 ) {
			state = State.PICKED;
		}
		if( stateOld<state ) {
			pickingUnitLoad.setState(state);
			firePacketStateChangeEvent(pickingUnitLoad, stateOld);
		}
		
		return pickingUnitLoad;
	}
	
	
	public PickingOrder recalculatePickingOrderState( PickingOrder pickingOrder ) throws FacadeException {
		String logStr = "recalculatePickingOrderState ";
		log.debug(logStr);
		
		if( pickingOrder == null ) {
			log.warn(logStr+"No order given. cannot calculate state");
			return null;
		}

		if( pickingOrder.getLines().size()==0 ) {
			log.debug(logStr+"do not force calculation on orders with no positions");
			return pickingOrder;
		}

		int stateOld = pickingOrder.getState();
		
		boolean hasOnlyCanceled = true;
		boolean hasOnlyFinished = true;
		boolean hasProcessed = false;
		for( PickingOrderLine pick : pickingOrder.getLines() ) {
			if( pick.getState() < State.CANCELED ) {
				hasOnlyCanceled = false;
			}
			
			if( pick.getState() < State.PICKED ) {
				hasOnlyFinished = false;
			}
			
			if( pick.getState() >= State.STARTED && pick.getState() != State.CANCELED ) {
				hasProcessed = true;
			}
		}
		
		int stateNew = pickingOrder.getState();
		if( hasOnlyCanceled ) {
			if( stateNew < State.CANCELED ) {
				stateNew = State.CANCELED;
			}
		}
		else if( hasOnlyFinished ) {
			if( stateNew < State.FINISHED ) {
				stateNew = State.FINISHED;
			}
		}
		else if( hasProcessed ) {
			if( stateNew < State.STARTED ) {
				stateNew = State.STARTED;
			}
		}
			
		if( stateNew!=pickingOrder.getState() ) {
			log.info(logStr+"Change state of order: old="+pickingOrder.getState()+", new="+stateNew);
			pickingOrder.setState(stateNew);
		}
		
		if( pickingOrder.getState() != stateOld )  {
			firePickingOrderStateChangeEvent(pickingOrder, stateOld);
		}
		return pickingOrder;
	}
	
	
	
	private void releaseReservation( StockUnit stock, BigDecimal amount) throws FacadeException {
		BigDecimal amountReservedNew = stock.getReservedAmount().subtract(amount);
		if( BigDecimal.ZERO.compareTo(amountReservedNew) > 0 ) {
			amountReservedNew = BigDecimal.ZERO;
		}
		stock.setReservedAmount(amountReservedNew);
	}
	
	private StockUnit moveStock(StockUnit pickFromStock, BigDecimal amountPicked, UnitLoad pickToUnitLoad, String activityCode) throws FacadeException {
		return inventoryBusiness.transferStock(pickFromStock, pickToUnitLoad, amountPicked, StockState.PICKED,activityCode, null, null);
	}

	private void changeAmount( StockUnit stock, BigDecimal amount, String activityCode ) throws FacadeException {
		inventoryBusiness.changeAmount(stock, amount, null, activityCode, null, null, true);
	}

	private void fireDeliveryOrderStateChangeEvent(DeliveryOrder entity, int oldState) throws BusinessException {
		try {
			log.debug("Fire DeliveryOrderStateChangeEvent. entity=" + entity + ", state=" + entity.getState()
					+ ", oldState=" + oldState);
			deliveryOrderStateChangeEvent.fire(new DeliveryOrderStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

	private void fireDeliveryOrderLineStateChangeEvent(DeliveryOrderLine entity, int oldState)
			throws BusinessException {
		try {
			log.debug("Fire DeliveryOrderLineStateChangeEvent. entity=" + entity + ", state=" + entity.getState()
					+ ", oldState=" + oldState);
			deliveryOrderLineStateChangeEvent
					.fire(new DeliveryOrderLineStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

	private void firePacketStateChangeEvent(Packet entity, int oldState) throws BusinessException {
		try {
			log.debug("Fire PacketStateChangeEvent. entity=" + entity + ", state=" + entity.getState() + ", oldState="
					+ oldState);
			packetStateChangeEvent.fire(new PacketStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

	private void firePickingOrderLineStateChangeEvent(PickingOrderLine entity, int oldState) throws BusinessException {
		try {
			log.debug("Fire PickingOrderLineStateChangeEvent. entity=" + entity + ", state=" + entity.getState()
					+ ", oldState=" + oldState);
			pickingOrderLineStateChangeEvent
					.fire(new PickingOrderLineStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

	private void firePickingOrderStateChangeEvent(PickingOrder entity, int oldState) throws BusinessException {
		try {
			log.debug("Fire PickingOrderStateChangeEvent. entity=" + entity + ", state=" + entity.getState()
					+ ", oldState=" + oldState);
			pickingOrderStateChangeEvent.fire(new PickingOrderStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

}
