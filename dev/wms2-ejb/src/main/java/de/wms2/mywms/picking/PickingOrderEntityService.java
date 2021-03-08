/* 
Copyright 2019 Matthias Krane
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
package de.wms2.mywms.picking;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.mywms.model.Client;

import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.entity.PersistenceManager;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.sequence.SequenceBusiness;
import de.wms2.mywms.strategy.OrderStrategy;

/**
 * @author krane
 *
 */
@Stateless
public class PickingOrderEntityService {

	@Inject
	private PersistenceManager manager;
	@Inject
	private SequenceBusiness sequenceBusiness;

	public PickingOrder create(Client client, OrderStrategy strategy) throws BusinessException {

		String number = sequenceBusiness.readNextValue(PickingOrder.class, "orderNumber");

		PickingOrder order = manager.createInstance(PickingOrder.class);

		order.setClient(client);
		order.setOrderNumber(number);
		order.setOrderStrategy(strategy);
		order.setLines(new ArrayList<>());
		if (strategy != null) {
			order.setCreateFollowUpPicks(strategy.isCreateFollowUpPicks());
			order.setDestination(strategy.getDefaultDestination());
		}

		manager.persistValidated(order);

		return order;
	}

	public PickingOrder readByOrderNumber(String orderNumber) {
		String jpql = "SELECT entity from " + PickingOrder.class.getName() + " entity ";
		jpql += " WHERE entity.orderNumber=:orderNumber";
		Query query = manager.createQuery(jpql);
		query.setParameter("orderNumber", orderNumber);
		try {
			return (PickingOrder) query.getSingleResult();
		} catch (NoResultException e) {
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<PickingOrder> readByDeliveryOrder(DeliveryOrder deliveryOrder) {
		String jpql = "SELECT DISTINCT pick.pickingOrder FROM " + PickingOrderLine.class.getName() + " pick ";
		jpql += " WHERE pick.deliveryOrderLine.deliveryOrder=:deliveryOrder";
		Query query = manager.createQuery(jpql);
		query.setParameter("deliveryOrder", deliveryOrder);
		List<PickingOrder> res = query.getResultList();
		return res;
	}

	/**
	 * Select a list of entities matching the given criteria. All parameters are
	 * optional.
	 */
	@SuppressWarnings("unchecked")
	public List<PickingOrder> readList(String externalId, Integer minState, Integer maxState) {
		String jpql = "SELECT entity FROM ";
		jpql += PickingOrder.class.getName() + " entity ";
		jpql += "WHERE 1=1 ";
		if (minState != null) {
			jpql += " and entity.state>=:minState ";
		}
		if (maxState != null) {
			jpql += " and entity.state<=:maxState ";
		}
		if (!StringUtils.isEmpty(externalId)) {
			jpql += " and entity.externalId=:externalId";
		}
		jpql += " order by entity.orderNumber ";
		Query query = manager.createQuery(jpql);
		if (minState != null) {
			query.setParameter("minState", minState);
		}
		if (maxState != null) {
			query.setParameter("maxState", maxState);
		}
		if (!StringUtils.isEmpty(externalId)) {
			query.setParameter("externalId", externalId);
		}
		return query.getResultList();
	}

}
