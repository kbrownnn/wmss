/*
 * Copyright (c) 2006 - 2013 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.facade;

import java.io.Serializable;

import de.wms2.mywms.shipping.ShippingOrder;

public class LOSGoodsOutTO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	private String nextLocationName;
	private String nextUnitLoadLabelId;
	private long numPosOpen = 0L;
	private long numPosDone = 0L;
	private boolean finished = false;
	private String externalNumber;
	private String courier;
	private String groupName;
	private String comment;
	private String orderNumber;
	private Long orderId;
	private String destination;
	
	public LOSGoodsOutTO( ShippingOrder order ) {
		this.externalNumber = order.getExternalNumber();
		this.courier = order.getCarrierName();
		this.comment = order.getAdditionalContent();
		this.orderNumber = order.getOrderNumber();
		this.orderId = order.getId();
		this.destination = (order.getDestination() == null ? null : order.getDestination().getName());
	}


	public String getNextLocationName() {
		return nextLocationName;
	}

	public void setNextLocationName(String nextLocationName) {
		this.nextLocationName = nextLocationName;
	}

	public String getNextUnitLoadLabelId() {
		return nextUnitLoadLabelId;
	}

	public void setNextUnitLoadLabelId(String nextUnitLoadLabelId) {
		this.nextUnitLoadLabelId = nextUnitLoadLabelId;
	}

	public long getNumPosOpen() {
		return numPosOpen;
	}

	public void setNumPosOpen(long numPosOpen) {
		this.numPosOpen = numPosOpen;
	}

	public long getNumPosDone() {
		return numPosDone;
	}

	public void setNumPosDone(long numPosDone) {
		this.numPosDone = numPosDone;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	public String getExternalNumber() {
		return externalNumber;
	}

	public void setExternalNumber(String externalNumber) {
		this.externalNumber = externalNumber;
	}

	public String getCourier() {
		return courier;
	}

	public void setCourier(String courier) {
		this.courier = courier;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}


	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}


	public String getOrderNumber() {
		return orderNumber;
	}
	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}


	public Long getOrderId() {
		return orderId;
	}
	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}


	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
}
