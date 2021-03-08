/*
 * Copyright (c) 2006 - 2010 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.mywms.model.Client;

import de.linogistix.los.common.exception.UnAuthorizedException;
import de.linogistix.los.util.BusinessObjectHelper;
import de.linogistix.los.util.businessservice.ContextService;
import de.wms2.mywms.client.ClientBusiness;
import de.wms2.mywms.product.ItemData;
import de.wms2.mywms.product.ItemDataNumber;
import de.wms2.mywms.product.ItemDataNumberEntityService;

@Stateless
public class QueryItemDataServiceBean 
	implements QueryItemDataService, QueryItemDataServiceRemote 
{

	@Inject
	private ClientBusiness clientService;
	
	@EJB
	private ContextService ctxService;
	
	@Inject
	private ItemDataNumberEntityService idnService;
	
	@PersistenceContext(unitName="myWMS")
	private EntityManager manager;
	
	/*
	 * (non-Javadoc)
	 * @see de.linogistix.los.inventory.service.QueryItemDataService#getByItemNumber(org.mywms.model.Client, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public ItemData getByItemNumber(Client client, String itemNumber) {
		
		Client callersClient = ctxService.getCallersClient();
        if (!callersClient.isSystemClient()) {
        	client = callersClient;
        }
        
		StringBuffer sb = new StringBuffer();
        sb.append("SELECT id FROM ");
        sb.append(ItemData.class.getSimpleName()+ " id ");
        sb.append("WHERE id.number=:itemNumber ");
        if( client != null ) {
            sb.append(" AND id.client = :cl ");
        }
        
        Query query = manager.createQuery(sb.toString());
        
        query.setParameter("itemNumber", itemNumber);
        if( client != null ) {
        	query.setParameter("cl", client);
        }

        List<ItemData> idList = null;
        try {
        	idList = query.getResultList();
        }
        catch (NoResultException ex) {
        	// is handled below
        }
        
        if( idList != null && idList.size() == 1 ) {
        	return (ItemData)BusinessObjectHelper.eagerRead(idList.get(0));
        }
        else if( idList == null || idList.size() == 0 ) {
        	// Try to find some ItemData with the additional numbers
			List<ItemDataNumber> idns = idnService.readByNumber(itemNumber);
			if (idns.size() == 1) {
				return (ItemData) BusinessObjectHelper.eagerRead(idns.get(0).getItemData());
			}
        }
        
    	return null;
	}
	
	@SuppressWarnings("unchecked")
	private List<ItemData> getListByItemNumber(Client client, String itemNumber) {
		
		Client callersClient = ctxService.getCallersClient();
        if (!callersClient.isSystemClient()) {
        	client = callersClient;
        }
        
		StringBuffer sb = new StringBuffer();
        sb.append("SELECT id FROM ");
        sb.append(ItemData.class.getSimpleName()+ " id ");
        sb.append("WHERE id.number=:itemNumber ");
        if( client != null ) {
            sb.append(" AND id.client = :cl ");
        }
        
        Query query = manager.createQuery(sb.toString());
        
        query.setParameter("itemNumber", itemNumber);
        if( client != null ) {
        	query.setParameter("cl", client);
        }

        List<ItemData> idList = null;
        try {
        	idList = query.getResultList();
        }
        catch (NoResultException ex) {
        	// is handled below
        }
        
        if( idList != null && idList.size() > 0 ) {
        	return idList;
        }
        
    	// Try to find some ItemData with the additional numbers
    	List<ItemDataNumber> idnList = idnService.readByNumber(itemNumber);
    	if( idnList != null && idnList.size() > 0 ) {
    		List<ItemData> retList = new ArrayList<ItemData>();
    		for( ItemDataNumber idn : idnList ) {
    			retList.add(idn.getItemData());
    		}
    		return retList;
    	}
    	
    	return null;
	}
	
	public ItemData getByItemNumber(String itemNumber) {
		return getByItemNumber(null, itemNumber);
	}

	public List<ItemData> getListByItemNumber(String itemNumber) {
		return getListByItemNumber(null, itemNumber);
	}

	/*
	 * (non-Javadoc)
	 * @see de.linogistix.los.inventory.service.QueryItemDataService#getItemNumbers()
	 */
	@SuppressWarnings("unchecked")
	public List<ClientItemNumberTO> getItemNumbers() throws UnAuthorizedException {
		
		if(ctxService.getCallersUser() == null ||
		   !ctxService.getCallersClient().equals(clientService.getSystemClient()))
		{
			throw new UnAuthorizedException();
		}
		
		StringBuffer sb = new StringBuffer("SELECT new de.linogistix.los.inventory.service.ClientItemNumberTO");
		sb.append("(it.client.number, it.number)");
		sb.append(" FROM "+ItemData.class.getSimpleName()+" it ");
		
		Query query = manager.createQuery(sb.toString());
		
		return query.getResultList();
	}

	

}
