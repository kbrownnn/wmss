/*
 * Copyright (c) 2012 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.mywms.model.BasicEntity;

import de.wms2.mywms.product.ItemData;

/**
 * @author krane
 *
 */
@Entity
@Table(name = "los_bom", uniqueConstraints = { 
		@UniqueConstraint(columnNames = {
				"parent_id","child_id" }) })
public class LOSBom extends BasicEntity {
	
	private static final long serialVersionUID = 1L;

	@ManyToOne(optional = false)
	private ItemData parent;

	@ManyToOne(optional = false)
	private ItemData child;

	@Column(nullable = false, precision = 17, scale = 4)
	private BigDecimal amount = BigDecimal.ONE;
	private int index = 0;
	private boolean pickable = true;
	
	public ItemData getParent() {
		return parent;
	}
	public void setParent(ItemData parent) {
		this.parent = parent;
	}
	
	public ItemData getChild() {
		return child;
	}
	public void setChild(ItemData child) {
		this.child = child;
	}
	
	public BigDecimal getAmount() {
		if (child != null){
			try{
				return this.amount.setScale(child.getScale());
			}catch(Throwable t){}
		}

		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	
	public boolean isPickable() {
		return pickable;
	}
	public void setPickable(boolean pickable) {
		this.pickable = pickable;
	}
	
	
	
	
}
