/*
 * Copyright (c) 2010 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */

package de.linogistix.los.inventory.query;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import de.linogistix.los.inventory.query.dto.ItemDataNumberTO;
import de.linogistix.los.query.BusinessObjectQueryBean;
import de.linogistix.los.query.TemplateQueryWhereToken;
import de.wms2.mywms.product.ItemDataNumber;

/**
 *
 * @author krane
 */
@Stateless
public class ItemDataNumberQueryBean extends BusinessObjectQueryBean<ItemDataNumber> implements ItemDataNumberQueryRemote{

	private static final String[] dtoProps = new String[] { "id", "version", "number",
		"orderIndex",
		"itemData.number" , "itemData.name"};

	@Override
	protected String[] getBODTOConstructorProps() {
		return dtoProps;
	}

	@Override
    public String getUniqueNameProp() {
        return "number";
    }
	@Override
	public Class<ItemDataNumberTO> getBODTOClass() {
		return ItemDataNumberTO.class;
	}

    @Override
	protected List<TemplateQueryWhereToken> getAutoCompletionTokens(String value) {
		List<TemplateQueryWhereToken> ret =  new ArrayList<TemplateQueryWhereToken>();
		
		TemplateQueryWhereToken number = new TemplateQueryWhereToken(
				TemplateQueryWhereToken.OPERATOR_LIKE, "number",
				value);
		number.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
		ret.add(number);

		TemplateQueryWhereToken iName = new TemplateQueryWhereToken(
				TemplateQueryWhereToken.OPERATOR_LIKE, "itemData.name",
				value);
		iName.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
		ret.add(iName);
		
		TemplateQueryWhereToken iNumber = new TemplateQueryWhereToken(
				TemplateQueryWhereToken.OPERATOR_LIKE, "itemData.number",
				value);
		iNumber.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
		ret.add(iNumber);

		return ret;
	}

}
