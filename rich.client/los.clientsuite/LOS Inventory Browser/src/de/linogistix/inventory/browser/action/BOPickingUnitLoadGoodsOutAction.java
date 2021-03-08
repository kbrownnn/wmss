/*
 * Copyright (c) 2012 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.inventory.browser.action;

import de.linogistix.common.bobrowser.bo.BO;
import de.linogistix.common.bobrowser.bo.BOMasterNode;
import de.linogistix.inventory.res.InventoryBundleResolver;
import de.linogistix.common.services.J2EEServiceLocator;
import de.linogistix.common.userlogin.LoginService;
import de.linogistix.common.util.CursorControl;
import de.linogistix.common.util.ExceptionAnnotator;
import de.linogistix.los.inventory.facade.LOSGoodsOutFacade;
import de.linogistix.los.inventory.query.dto.LOSPickingUnitLoadTO;
import de.linogistix.los.model.State;
import java.util.ArrayList;
import java.util.List;
import org.mywms.facade.FacadeException;
import org.mywms.globals.Role;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author krane
 */
public final class BOPickingUnitLoadGoodsOutAction extends NodeAction {

    private static String[] allowedRoles = new String[]{
        Role.ADMIN.toString(),Role.INVENTORY_STR,Role.FOREMAN_STR
    };



    public String getName() {
        return NbBundle.getMessage(InventoryBundleResolver.class, "CreateGoodsOutOrder.name");
    }

    protected String iconResource() {
        return "de/linogistix/bobrowser/res/icon/Action.png";
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    protected boolean asynchronous() {
        return false;
    }

    protected boolean enable(Node[] activatedNodes) {
        LoginService login = (LoginService) Lookup.getDefault().lookup(LoginService.class);
        boolean allowed =  login.checkRolesAllowed(allowedRoles);
        if( !allowed ) {
            return false;
        }

        try {
            for (Node n : activatedNodes) {
                if (n == null) {
                    continue;
                }
                if (!(n instanceof BOMasterNode)) {
                    continue;
                }

                LOSPickingUnitLoadTO unitLoad = (LOSPickingUnitLoadTO)((BOMasterNode)n).getEntity();
                if( unitLoad.getState()>=State.FINISHED ) {
                    return false;
                }
                if( unitLoad.getState()<State.PICKED ) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected void performAction(Node[] activatedNodes) {
        if (activatedNodes == null) {
            return;
        }

        NotifyDescriptor d = new NotifyDescriptor.Confirmation(
                NbBundle.getMessage(InventoryBundleResolver.class, "CreateGoodsOutOrder.text"),
                NbBundle.getMessage(InventoryBundleResolver.class, "CreateGoodsOutOrder.title"),
                NotifyDescriptor.OK_CANCEL_OPTION);

        if (DialogDisplayer.getDefault().notify(d) != NotifyDescriptor.OK_OPTION) {
            return;
        }

        BOMasterNode node = null;
        try {
            J2EEServiceLocator loc = (J2EEServiceLocator) Lookup.getDefault().lookup(J2EEServiceLocator.class);
            LOSGoodsOutFacade outFacade = loc.getStateless(LOSGoodsOutFacade.class);
            List<Long> idList = new ArrayList<Long>();

            for (Node n : activatedNodes) {
                if( n == null || !(n instanceof BOMasterNode)) {
                    continue;
                }
                node = (BOMasterNode)n;
                LOSPickingUnitLoadTO unitLoad = (LOSPickingUnitLoadTO)node.getEntity();
                idList.add(unitLoad.getId());
            }
            outFacade.createGoodsOutOrder(idList);
            
        } catch (FacadeException ex) {
            ExceptionAnnotator.annotate(ex);
        } finally {
            CursorControl.showNormalCursor();
        }
        
        if( node != null ) {
            BO bo = node.getBo();
            bo.fireOutdatedEvent(node);
        }

    }
}

