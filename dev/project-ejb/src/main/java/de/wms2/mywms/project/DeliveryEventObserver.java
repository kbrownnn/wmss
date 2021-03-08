/* 
Copyright 2019-2020 Matthias Krane
info@krane.engineer

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
package de.wms2.mywms.project;

import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.delivery.DeliveryOrderStateChangeEvent;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.picking.Packet;
import de.wms2.mywms.picking.PickingOrder;
import de.wms2.mywms.picking.PickingOrderStateChangeEvent;
import de.wms2.mywms.shipping.ShippingBusiness;
import de.wms2.mywms.shipping.ShippingOrder;
import de.wms2.mywms.strategy.OrderState;
import de.wms2.mywms.strategy.OrderStrategy;

/**
 * This observer synchronizes the picking, packing and shipping processes.
 * 
 * @author krane
 *
 */
public class DeliveryEventObserver {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Inject
	private ShippingBusiness shippingBusiness;

	public void listen(@Observes DeliveryOrderStateChangeEvent event) throws BusinessException {
		if (event == null || event.getDeliveryOrder() == null) {
			return;
		}

		DeliveryOrder deliveryOrder = event.getDeliveryOrder();
		int oldState = event.getOldState();
		int newState = event.getNewState();

		if (oldState < OrderState.SHIPPING && newState == OrderState.SHIPPING) {
			logger.info("DeliveryOrder got state SHIPPING. deliveryOrder=" + deliveryOrder + ", oldState=" + oldState
					+ ", newState=" + newState);

			OrderStrategy orderStrategy = deliveryOrder.getOrderStrategy();

			if (orderStrategy.isCreateShippingOrder()) {
				logger.info("Create shipping order for delivery deliveryOrder=" + deliveryOrder + ", orderStrategy="
						+ orderStrategy);
				ShippingOrder shippingOrder = shippingBusiness.createOrder(deliveryOrder);
				if (shippingOrder != null) {
					shippingBusiness.releaseOperation(shippingOrder);
				}
			}
		}

	}

	public void listen(@Observes PickingOrderStateChangeEvent event) throws BusinessException {
		if (event == null || event.getPickingOrder() == null) {
			return;
		}

		PickingOrder pickingOrder = event.getPickingOrder();
		int oldState = event.getOldState();
		int newState = event.getNewState();
		if (oldState < OrderState.FINISHED && newState == OrderState.FINISHED) {
			logger.info("PickingOrder got state FINISHED. pickingOrder=" + pickingOrder + ", oldState=" + oldState
					+ ", newState=" + newState);
			DeliveryOrder deliveryOrder = pickingOrder.getDeliveryOrder();
			if (deliveryOrder == null) {
				// Standalone picking order.
				// Generate shipping oder if strategy flag is set.
				// Packing is not defined for picking orders without delivery order.
				OrderStrategy orderStrategy = pickingOrder.getOrderStrategy();

				if (orderStrategy.isCreateShippingOrder()) {
					logger.info("Create shipping order for pickingOrder=" + pickingOrder + ", orderStrategy="
							+ orderStrategy);
					ShippingOrder shippingOrder = null;
					for (Packet packet : pickingOrder.getPackets()) {
						if (packet.getState() >= OrderState.SHIPPING) {
							logger.info("Packet alread in shipping. packet=" + packet + ", pickingOrder=" + pickingOrder
									+ ", orderStrategy=" + orderStrategy);
							continue;
						}
						if (shippingOrder == null) {
							shippingOrder = shippingBusiness.createOrder(pickingOrder.getClient());
							shippingOrder.setAddress(pickingOrder.getAddress());
							shippingOrder.setCarrierName(pickingOrder.getCarrierName());
							shippingOrder.setCarrierService(pickingOrder.getCarrierService());
							shippingOrder.setExternalNumber(pickingOrder.getExternalNumber());
						}
						shippingBusiness.addLine(shippingOrder, packet);
					}
					if (shippingOrder != null) {
						shippingBusiness.releaseOperation(shippingOrder);
					}
				}
			}

		}

	}

}
